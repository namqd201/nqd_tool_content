package com.nqd.nqd_tool_content.worker;

import com.nqd.nqd_tool_content.entity.Post;
import com.nqd.nqd_tool_content.entity.PostAttempt;
import com.nqd.nqd_tool_content.repository.PostAttemptRepository;
import com.nqd.nqd_tool_content.repository.PostRepository;
import com.nqd.nqd_tool_content.service.NotificationService;
import com.nqd.nqd_tool_content.service.social.SocialAdapterRegistry;
import com.nqd.nqd_tool_content.service.social.SocialPlatformAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class Reconciler {

    private final PostRepository postRepository;
    private final PostAttemptRepository postAttemptRepository;
    private final NotificationService notificationService;
    private final SocialAdapterRegistry adapterRegistry;

    /**
     * Polls every 30 seconds for posts in RECONCILING status whose next_attempt_at <= now.
     */
    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void runReconciliationCycle() {
        LocalDateTime now = LocalDateTime.now();
        List<Post> reconcilingPosts = postRepository
                .findByStatusAndNextAttemptAtLessThanEqualAndIsDeletedFalse("RECONCILING", now);

        if (reconcilingPosts.isEmpty()) {
            return;
        }

        log.info("[Reconciler] Reconciling {} posts with uncertain publication outcome", reconcilingPosts.size());

        for (Post post : reconcilingPosts) {
            try {
                reconcilePost(post);
            } catch (Exception e) {
                log.error("[Reconciler] Error reconciling post {}: {}", post.getId(), e.getMessage(), e);
            }
        }
    }

    public void reconcilePost(Post post) {
        LocalDateTime now = LocalDateTime.now();
        int cycle = (post.getPublishCycle() != null ? post.getPublishCycle() : 1);

        // 1. Check if post_attempts has a SUCCESS record for this cycle
        List<PostAttempt> attempts = postAttemptRepository.findByPostIdOrderByAttemptNoAsc(post.getId());
        boolean hasSuccessAttempt = attempts.stream()
                .anyMatch(a -> a.getPublishCycle() != null && a.getPublishCycle() == cycle && "SUCCESS".equalsIgnoreCase(a.getOutcome()));

        if (hasSuccessAttempt) {
            log.info("[Reconciler] Post {} found SUCCESS attempt in cycle {} -> PUBLISHED", post.getId(), cycle);
            post.setStatus("PUBLISHED");
            post.setErrorClass("NONE");
            post.setPublishedAt(now);
            postRepository.save(post);
            return;
        }

        // 2. Kiểm tra nếu nền tảng không hỗ trợ tra cứu đối soát (Mục 15.7 step 2)
        Optional<SocialPlatformAdapter> adapterOpt = adapterRegistry.getAdapter(post.getPlatform());
        if (adapterOpt.isPresent() && !adapterOpt.get().supportsPublishedPostLookup()) {
            log.warn("[Reconciler] Post {} platform {} does not support post lookup -> NEEDS_REVIEW immediately",
                    post.getId(), post.getPlatform());
            post.setStatus("NEEDS_REVIEW");
            post.setErrorClass("UNCERTAIN");
            post.setNeedsReviewReason("Nền tảng " + post.getPlatform() + " không hỗ trợ tra cứu bài đã đăng. Cần kiểm tra thủ công.");
            postRepository.save(post);

            notificationService.enqueue(
                    post.getUserId(),
                    "POST_NEEDS_REVIEW",
                    "Cần kiểm tra trạng thái bài đăng",
                    "Hệ thống không thể tự động tra cứu kết quả trên " + post.getPlatform() + ". Vui lòng kiểm tra và đánh dấu đã đăng nếu bài đã xuất hiện.",
                    post.getId(),
                    post.getPlanId()
            );
            return;
        }

        // 3. Increment reconcile_count
        int count = (post.getReconcileCount() != null ? post.getReconcileCount() : 0) + 1;
        post.setReconcileCount(count);

        if (count < 2) {
            // First check: schedule another lookup in 2 minutes
            post.setNextAttemptAt(now.plusMinutes(2));
            postRepository.save(post);
            log.info("[Reconciler] Post {} reconcile check #1: rechecking in 2 minutes", post.getId());
        } else if (count == 2) {
            // Second consecutive NOT_FOUND: treat as definitely NOT published -> READY for safe retry
            int attemptCount = (post.getAttemptCount() != null ? post.getAttemptCount() : 1);
            int maxAttempts = (post.getMaxAttempts() != null ? post.getMaxAttempts() : 3);

            if (attemptCount < maxAttempts) {
                log.info("[Reconciler] Post {} confirmed not published after 2 checks -> returning to READY for retry", post.getId());
                post.setStatus("READY");
                post.setNextAttemptAt(now.plusMinutes(1));
                post.setErrorClass("TRANSIENT_SAFE");
            } else {
                log.warn("[Reconciler] Post {} exhausted max attempts after reconciliation -> FAILED", post.getId());
                post.setStatus("FAILED");
                post.setErrorClass("RETRY_LIMIT_EXCEEDED");
                post.setLastError("Xác nhận chưa đăng nhưng đã hết số lần thử lại.");
            }
            postRepository.save(post);
        } else {
            // More than 2 checks or lookup failed repeatedly -> NEEDS_REVIEW
            log.warn("[Reconciler] Post {} cannot be automatically reconciled -> NEEDS_REVIEW", post.getId());
            post.setStatus("NEEDS_REVIEW");
            post.setNeedsReviewReason("Không thể xác định bài viết đã được đăng trên " + post.getPlatform() + " hay chưa. Cần người dùng kiểm tra thủ công.");
            postRepository.save(post);

            notificationService.enqueue(
                    post.getUserId(),
                    "POST_NEEDS_REVIEW",
                    "Cần kiểm tra trạng thái bài đăng",
                    "Hệ thống không thể xác minh kết quả đăng bài trên " + post.getPlatform() + ". Vui lòng kiểm tra trên mạng xã hội và đánh dấu.",
                    post.getId(),
                    post.getPlanId()
            );
        }
    }
}
