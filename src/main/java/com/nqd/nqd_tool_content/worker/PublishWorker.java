package com.nqd.nqd_tool_content.worker;

import com.nqd.nqd_tool_content.entity.*;
import com.nqd.nqd_tool_content.repository.*;
import com.nqd.nqd_tool_content.service.NotificationService;
import com.nqd.nqd_tool_content.service.social.SocialAdapterRegistry;
import com.nqd.nqd_tool_content.service.social.SocialPlatformAdapter;
import com.nqd.nqd_tool_content.service.social.dto.PostResult;
import com.nqd.nqd_tool_content.service.social.dto.PublishRequest;
import com.nqd.nqd_tool_content.service.storage.StorageService;
import com.nqd.nqd_tool_content.util.CryptoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class PublishWorker {

    private final PostRepository postRepository;
    private final PostAttemptRepository postAttemptRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final SocialConnectionRepository socialConnectionRepository;
    private final PostMediaRepository postMediaRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final SocialAdapterRegistry adapterRegistry;
    private final CryptoUtil cryptoUtil;
    private final StorageService storageService;
    private final NotificationService notificationService;
    private final com.nqd.nqd_tool_content.service.social.PlatformRateLimiter rateLimiter;

    @Value("${app.publish.dry-run:false}")
    private boolean dryRun;

    /**
     * Executes the publish lifecycle for a single claimed Post.
     */
    public void processPost(UUID postId, String workerId) {
        log.info("[PublishWorker] Worker {} starting execution for post {}", workerId, postId);

        Post post = postRepository.findById(postId).orElse(null);
        if (post == null || post.getIsDeleted()) {
            log.warn("[PublishWorker] Post {} not found or deleted", postId);
            return;
        }

        // 1. Verify preconditions
        LocalDateTime now = LocalDateTime.now();
        int missedGrace = (post.getMissedGraceMinutes() != null) ? post.getMissedGraceMinutes() : 120;
        if (post.getScheduledAt() != null && now.isAfter(post.getScheduledAt().plusMinutes(missedGrace))) {
            log.warn("[PublishWorker] Post {} missed grace window (scheduled: {}, grace: {}m)",
                    postId, post.getScheduledAt(), missedGrace);
            markMissed(post, "Missed scheduled window by more than " + missedGrace + " minutes");
            return;
        }

        SocialAccount account = socialAccountRepository.findById(post.getSocialAccountId()).orElse(null);
        if (account == null || !Boolean.TRUE.equals(account.getIsEnabled()) || !"ACTIVE".equalsIgnoreCase(account.getStatus())) {
            log.error("[PublishWorker] Post {} channel is not active or enabled", postId);
            markFailed(post, "ACCOUNT_NOT_ACTIVE", "Kênh mạng xã hội bị tắt hoặc không hoạt động", false);
            return;
        }

        SocialConnection connection = socialConnectionRepository.findById(account.getConnectionId()).orElse(null);
        if (connection == null || !"ACTIVE".equalsIgnoreCase(connection.getStatus())) {
            log.error("[PublishWorker] Post {} connection is not active", postId);
            markFailed(post, "CONNECTION_NOT_ACTIVE", "Kết nối tài khoản mạng xã hội không hoạt động", false);
            return;
        }

        // 2. Rate limit check (ADAPT-006)
        if (!rateLimiter.tryAcquire(account.getId(), post.getPlatform())) {
            log.warn("[PublishWorker] Account {} rate limit exceeded. Deferring post {} by 2 minutes.", account.getId(), postId);
            post.setStatus("READY");
            post.setNextAttemptAt(now.plusMinutes(2));
            post.setLastError("Rate limit exceeded on platform. Deferred for 2 minutes.");
            post.setLockedBy(null);
            post.setLeaseExpiresAt(null);
            postRepository.save(post);
            return;
        }

        // 3. Prepare decrypted token
        String accessToken;
        String pageAccessToken = null;
        try {
            accessToken = cryptoUtil.decrypt(connection.getAccessTokenEnc(), connection.getId() + ":access_token");
            if (account.getPageAccessTokenEnc() != null) {
                try {
                    pageAccessToken = cryptoUtil.decrypt(account.getPageAccessTokenEnc(), connection.getId() + ":page_access_token");
                } catch (Exception ex) {
                    pageAccessToken = cryptoUtil.decrypt(account.getPageAccessTokenEnc(), account.getId() + ":page_access_token");
                }
            }
        } catch (Exception e) {
            log.error("[PublishWorker] Failed to decrypt tokens for post {}: {}", postId, e.getMessage());
            markFailed(post, "AUTH_DECRYPT_FAILED", "Không thể giải mã token: " + e.getMessage(), false);
            return;
        }

        // 3. Resolve Media URLs
        List<String> mediaUrls = new ArrayList<>();
        List<PostMedia> postMedias = postMediaRepository.findByPostIdOrderByOrderIndexAsc(postId);
        for (PostMedia pm : postMedias) {
            mediaAssetRepository.findById(pm.getMediaAssetId()).ifPresent(ma -> {
                String url = storageService.getPublicUrl(ma.getPublicToken());
                if (url != null) mediaUrls.add(url);
            });
        }

        // 4. Write-Ahead step (Record intent to call platform)
        int attemptNo = (post.getAttemptCount() != null ? post.getAttemptCount() : 1);
        int publishCycle = (post.getPublishCycle() != null ? post.getPublishCycle() : 1);

        PostAttempt attempt = recordWriteAhead(post, workerId, publishCycle, attemptNo);

        // 5. Call Adapter
        SocialPlatformAdapter adapter = adapterRegistry.getAdapter(post.getPlatform())
                .orElse(adapterRegistry.getAdapter("FAKE").orElseThrow());

        PublishRequest request = PublishRequest.builder()
                .platform(post.getPlatform())
                .platformAccountId(account.getPlatformAccountId())
                .accountType(account.getAccountType())
                .accessToken(accessToken)
                .pageAccessToken(pageAccessToken)
                .text(post.getContent() != null ? post.getContent() : "")
                .mediaUrls(mediaUrls)
                .dryRun(dryRun)
                .build();

        PostResult result;
        try {
            result = adapter.publish(request);
        } catch (Exception e) {
            log.error("[PublishWorker] Unhandled exception calling adapter for post {}: {}", postId, e.getMessage(), e);
            result = PostResult.needsReconcile("Lỗi không rõ kết quả từ mạng xã hội: " + e.getMessage(), null);
        }

        // 6. Handle Result
        handlePublishResult(post, attempt, result);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PostAttempt recordWriteAhead(Post post, String workerId, int publishCycle, int attemptNo) {
        LocalDateTime now = LocalDateTime.now();
        post.setRequestSentAt(now);
        postRepository.save(post);

        PostAttempt attempt = PostAttempt.builder()
                .postId(post.getId())
                .publishCycle(publishCycle)
                .attemptNo(attemptNo)
                .workerId(workerId)
                .startedAt(now)
                .requestSentAt(now)
                .outcome("PENDING")
                .build();

        return postAttemptRepository.save(attempt);
    }

    @Transactional
    public void handlePublishResult(Post post, PostAttempt attempt, PostResult result) {
        LocalDateTime now = LocalDateTime.now();
        attempt.setFinishedAt(now);
        attempt.setResponseSnippet(result.getRawResponse());
        attempt.setPlatformErrorCode(result.getErrorCode());
        attempt.setErrorMessage(result.getErrorMessage());

        if ("PUBLISHED".equalsIgnoreCase(result.getStatus())) {
            // Success
            attempt.setOutcome("SUCCESS");
            postAttemptRepository.save(attempt);

            post.setStatus("PUBLISHED");
            post.setPlatformPostId(result.getPlatformPostId());
            post.setPlatformPostUrl(result.getPostUrl());
            post.setPublishedAt(now);
            post.setErrorClass("NONE");
            post.setLastError(null);
            post.setLockedBy(null);
            post.setLeaseExpiresAt(null);
            postRepository.save(post);

            log.info("[PublishWorker] Post {} published successfully! URL: {}", post.getId(), result.getPostUrl());

            // Outbox notification
            notificationService.enqueue(
                    post.getUserId(),
                    "POST_PUBLISHED",
                    "Bài đăng đã được xuất bản",
                    String.format("Bài viết đã đăng thành công lên %s lúc %s.", post.getPlatform(), now),
                    post.getId(),
                    post.getPlanId()
            );

        } else if ("NEEDS_RECONCILE".equalsIgnoreCase(result.getStatus())) {
            // Uncertain outcome -> RECONCILING
            attempt.setOutcome("UNCERTAIN");
            postAttemptRepository.save(attempt);

            post.setStatus("RECONCILING");
            post.setErrorClass("UNCERTAIN");
            post.setLastError(result.getErrorMessage());
            post.setNextAttemptAt(now.plusMinutes(2)); // Reconciler polls after 2m
            post.setLockedBy(null);
            post.setLeaseExpiresAt(null);
            postRepository.save(post);

            log.warn("[PublishWorker] Post {} moved to RECONCILING due to uncertain outcome: {}",
                    post.getId(), result.getErrorMessage());

        } else {
            // FAILED or Retryable error
            if (result.isRetryable()) {
                handleRetryableError(post, attempt, result);
            } else {
                markFailed(post, result.getErrorCode(), result.getErrorMessage(), false);
                attempt.setOutcome("PERMANENT_ERROR");
                postAttemptRepository.save(attempt);
            }
        }
    }

    private void handleRetryableError(Post post, PostAttempt attempt, PostResult result) {
        LocalDateTime now = LocalDateTime.now();
        int attempts = (post.getAttemptCount() != null ? post.getAttemptCount() : 1);
        int maxAttempts = (post.getMaxAttempts() != null ? post.getMaxAttempts() : 3);
        int missedGrace = (post.getMissedGraceMinutes() != null) ? post.getMissedGraceMinutes() : 120;

        attempt.setOutcome("RETRYABLE_ERROR");
        postAttemptRepository.save(attempt);

        // Exponential backoff: 1m, 5m, 15m
        int backoffMinutes = attempts == 1 ? 1 : (attempts == 2 ? 5 : 15);
        LocalDateTime nextAttempt = now.plusMinutes(backoffMinutes);

        if (attempts < maxAttempts && post.getScheduledAt() != null && nextAttempt.isBefore(post.getScheduledAt().plusMinutes(missedGrace))) {
            post.setStatus("READY");
            post.setNextAttemptAt(nextAttempt);
            post.setErrorClass("TRANSIENT_SAFE");
            post.setLastError(result.getErrorMessage());
            post.setLockedBy(null);
            post.setLeaseExpiresAt(null);
            postRepository.save(post);

            log.info("[PublishWorker] Post {} scheduled for retry #{} at {}", post.getId(), attempts + 1, nextAttempt);
        } else {
            markFailed(post, "RETRY_LIMIT_EXCEEDED", "Đã hết số lần thử lại (" + maxAttempts + " lần): " + result.getErrorMessage(), false);
        }
    }

    @Transactional
    public void markMissed(Post post, String reason) {
        post.setStatus("MISSED");
        post.setLastError(reason);
        post.setLockedBy(null);
        post.setLeaseExpiresAt(null);
        postRepository.save(post);

        notificationService.enqueue(
                post.getUserId(),
                "POST_MISSED",
                "Bài đăng bị bỏ lỡ",
                "Bài viết đã quá hạn đăng lúc " + post.getScheduledAt() + ". Lý do: " + reason,
                post.getId(),
                post.getPlanId()
        );
    }

    @Transactional
    public void markFailed(Post post, String errorCode, String errorMessage, boolean notifyHighPriority) {
        post.setStatus("FAILED");
        post.setErrorClass(errorCode != null ? errorCode : "PERMANENT");
        post.setLastError(errorMessage);
        post.setLockedBy(null);
        post.setLeaseExpiresAt(null);
        postRepository.save(post);

        notificationService.enqueue(
                post.getUserId(),
                "POST_FAILED",
                "Đăng bài thất bại",
                "Không thể đăng bài lên " + post.getPlatform() + ": " + errorMessage,
                post.getId(),
                post.getPlanId()
        );
    }
}
