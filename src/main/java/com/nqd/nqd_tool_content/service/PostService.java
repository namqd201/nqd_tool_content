package com.nqd.nqd_tool_content.service;

import com.nqd.nqd_tool_content.dto.request.BulkPostActionRequest;
import com.nqd.nqd_tool_content.dto.request.MarkPublishedRequest;
import com.nqd.nqd_tool_content.dto.request.UpdatePostRequest;
import com.nqd.nqd_tool_content.dto.response.PostResponse;
import com.nqd.nqd_tool_content.dto.response.ScheduleSummaryResponse;
import com.nqd.nqd_tool_content.entity.*;
import com.nqd.nqd_tool_content.exception.BusinessException;
import com.nqd.nqd_tool_content.repository.*;
import com.nqd.nqd_tool_content.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PostAttemptRepository postAttemptRepository;
    private final ContentPlanRepository planRepository;
    private final SocialAccountRepository accountRepository;
    private final PostMediaRepository postMediaRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final StorageService storageService;
    private final com.nqd.nqd_tool_content.service.ai.impl.AIFactory aiFactory;
    private final com.nqd.nqd_tool_content.service.ai.BudgetService budgetService;
    private final com.nqd.nqd_tool_content.service.ai.ContentGuard contentGuard;
    private final com.nqd.nqd_tool_content.repository.UserSettingsRepository userSettingsRepository;

    public List<PostResponse> listPosts(UUID userId, String status, String platform, UUID planId, UUID accountId) {
        List<Post> posts = postRepository.findByUserIdAndIsDeletedFalseOrderByScheduledAtAsc(userId);

        return posts.stream()
                .filter(p -> status == null || status.isBlank() || p.getStatus().equalsIgnoreCase(status))
                .filter(p -> platform == null || platform.isBlank() || p.getPlatform().equalsIgnoreCase(platform))
                .filter(p -> planId == null || (p.getPlanId() != null && p.getPlanId().equals(planId)))
                .filter(p -> accountId == null || p.getSocialAccountId().equals(accountId))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public PostResponse getPost(UUID userId, UUID postId) {
        Post post = postRepository.findById(postId)
                .filter(p -> p.getUserId().equals(userId) && !p.getIsDeleted())
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy bài viết"));
        return mapToResponse(post);
    }

    @Transactional
    public PostResponse updatePost(UUID userId, UUID postId, UpdatePostRequest req) {
        Post post = postRepository.findById(postId)
                .filter(p -> p.getUserId().equals(userId) && !p.getIsDeleted())
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy bài viết"));

        if ("PUBLISHING".equalsIgnoreCase(post.getStatus()) || "RECONCILING".equalsIgnoreCase(post.getStatus())) {
            throw new BusinessException("POST_STATE_CONFLICT", "Không thể sửa bài đang trong quá trình xuất bản hoặc đối soát");
        }

        if (req.getContent() != null) {
            post.setContent(req.getContent());
            post.setContentEditedByUser(true);
        }
        if (req.getHashtags() != null) {
            post.setHashtags(req.getHashtags());
        }
        if (req.getScheduledAt() != null) {
            post.setScheduledAt(req.getScheduledAt());
            post.setNextAttemptAt(req.getScheduledAt());
            if ("PLANNED".equalsIgnoreCase(post.getStatus())) {
                int leadHours = 12;
                post.setGenerateAt(req.getScheduledAt().minusHours(leadHours));
            }
        }

        return mapToResponse(postRepository.save(post));
    }

    @Transactional
    public PostResponse publishNow(UUID userId, UUID postId) {
        Post post = postRepository.findById(postId)
                .filter(p -> p.getUserId().equals(userId) && !p.getIsDeleted())
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy bài viết"));

        if (!List.of("READY", "FAILED", "NEEDS_REVIEW").contains(post.getStatus())) {
            throw new BusinessException("POST_STATE_CONFLICT", "Chỉ có thể bấm Đăng ngay với bài READY, FAILED hoặc NEEDS_REVIEW");
        }

        // Open new publish cycle if coming from FAILED or NEEDS_REVIEW
        if ("FAILED".equalsIgnoreCase(post.getStatus()) || "NEEDS_REVIEW".equalsIgnoreCase(post.getStatus())) {
            post.setPublishCycle((post.getPublishCycle() != null ? post.getPublishCycle() : 1) + 1);
            post.setAttemptCount(0);
            post.setReconcileCount(0);
        }

        post.setStatus("READY");
        post.setNextAttemptAt(LocalDateTime.now());
        post.setLockedBy(null);
        post.setLeaseExpiresAt(null);

        return mapToResponse(postRepository.save(post));
    }

    @Transactional
    public PostResponse skipPost(UUID userId, UUID postId) {
        Post post = postRepository.findById(postId)
                .filter(p -> p.getUserId().equals(userId) && !p.getIsDeleted())
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy bài viết"));

        if (List.of("PUBLISHING", "RECONCILING", "PUBLISHED").contains(post.getStatus())) {
            throw new BusinessException("POST_STATE_CONFLICT", "Không thể bỏ qua bài viết đang xuất bản hoặc đã đăng thành công");
        }

        post.setStatus("SKIPPED");
        post.setLockedBy(null);
        post.setLeaseExpiresAt(null);

        return mapToResponse(postRepository.save(post));
    }

    @Transactional
    public PostResponse retryPost(UUID userId, UUID postId) {
        Post post = postRepository.findById(postId)
                .filter(p -> p.getUserId().equals(userId) && !p.getIsDeleted())
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy bài viết"));

        if (!List.of("FAILED", "NEEDS_REVIEW").contains(post.getStatus())) {
            throw new BusinessException("POST_STATE_CONFLICT", "Chỉ có thể thử lại bài viết ở trạng thái FAILED hoặc NEEDS_REVIEW");
        }

        post.setPublishCycle((post.getPublishCycle() != null ? post.getPublishCycle() : 1) + 1);
        post.setAttemptCount(0);
        post.setReconcileCount(0);
        post.setStatus("READY");
        post.setNextAttemptAt(LocalDateTime.now());
        post.setLockedBy(null);
        post.setLeaseExpiresAt(null);

        return mapToResponse(postRepository.save(post));
    }

    @Transactional
    public PostResponse markPublished(UUID userId, UUID postId, MarkPublishedRequest req) {
        Post post = postRepository.findById(postId)
                .filter(p -> p.getUserId().equals(userId) && !p.getIsDeleted())
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy bài viết"));

        if (!List.of("NEEDS_REVIEW", "FAILED").contains(post.getStatus())) {
            throw new BusinessException("POST_STATE_CONFLICT", "Chỉ có thể đánh dấu đã đăng với bài NEEDS_REVIEW hoặc FAILED");
        }

        LocalDateTime now = LocalDateTime.now();
        post.setStatus("PUBLISHED");
        post.setPlatformPostUrl(req.getUrl());
        post.setPublishedAt(now);
        post.setErrorClass("NONE");
        post.setLastError(null);
        post.setNeedsReviewReason(null);
        post.setLockedBy(null);
        post.setLeaseExpiresAt(null);

        return mapToResponse(postRepository.save(post));
    }

    @Transactional
    public PostResponse regenerateContent(UUID userId, UUID postId) {
        return regenerateContent(userId, postId, null);
    }

    @Transactional
    public PostResponse regenerateContent(
            UUID userId,
            UUID postId,
            com.nqd.nqd_tool_content.dto.request.RegeneratePostRequest customReq
    ) {
        Post post = postRepository.findById(postId)
                .filter(p -> p.getUserId().equals(userId) && !p.getIsDeleted())
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy bài viết"));

        if (!List.of("PLANNED", "READY", "GENERATION_FAILED").contains(post.getStatus())) {
            throw new BusinessException("POST_STATE_CONFLICT", "Chỉ có thể tạo lại nội dung bài PLANNED, READY hoặc GENERATION_FAILED");
        }

        UserSettings settings = userSettingsRepository.findByUserId(userId).orElse(null);
        ContentPlan plan = post.getPlanId() != null ? planRepository.findById(post.getPlanId()).orElse(null) : null;

        var textProvider = aiFactory.getTextProvider(settings);

        List<String> recentPosts = new ArrayList<>();
        if (post.getPlanId() != null) {
            List<Post> previousPlanPosts = postRepository.findByPlanIdAndIsDeletedFalseOrderByScheduledAtAsc(post.getPlanId());
            for (Post prev : previousPlanPosts) {
                if (prev.getContent() != null && !prev.getContent().isBlank() && !prev.getId().equals(post.getId())) {
                    String cleanSnippet = prev.getContent().replaceAll("#\\w+", "").replaceAll("\\s+", " ").trim();
                    if (cleanSnippet.length() > 140) {
                        cleanSnippet = cleanSnippet.substring(0, 140) + "...";
                    }
                    recentPosts.add((prev.getAngle() != null ? "[" + prev.getAngle() + "] " : "") + cleanSnippet);
                    if (recentPosts.size() >= 5) break;
                }
            }
        }

        String angle = (customReq != null && customReq.getAngle() != null && !customReq.getAngle().isBlank())
                ? customReq.getAngle() : post.getAngle();
        String tone = (customReq != null && customReq.getTone() != null && !customReq.getTone().isBlank())
                ? customReq.getTone()
                : (plan != null && plan.getTone() != null ? plan.getTone() : (settings != null ? settings.getTone() : null));

        var req = com.nqd.nqd_tool_content.service.ai.dto.AIContentRequest.builder()
                .topic(plan != null ? plan.getTopic() : "Bài viết chia sẻ kiến thức")
                .angle(angle)
                .instructions(plan != null ? plan.getInstructions() : "")
                .customPrompt(customReq != null ? customReq.getCustomPrompt() : null)
                .tone(tone)
                .language(plan != null && plan.getLanguage() != null ? plan.getLanguage() : "vi")
                .platforms(List.of(post.getPlatform()))
                .recentPosts(recentPosts)
                .generateImagePrompt(false)
                .build();

        long startTime = System.currentTimeMillis();
        var aiResponse = textProvider.generateContent(req);
        long latencyMs = System.currentTimeMillis() - startTime;

        budgetService.recordUsage(
                userId, "TEXT", "POST_REGENERATE", textProvider.getProviderName(),
                aiResponse.getModelUsed(), aiResponse.getPromptTokens(), aiResponse.getCompletionTokens(),
                0, latencyMs, "SUCCESS", null, aiResponse.getEstimatedCostUsd(), post.getId(), post.getPlanId()
        );

        if (aiResponse.getPosts() != null && !aiResponse.getPosts().isEmpty()) {
            var item = aiResponse.getPosts().get(0);
            post.setContent(item.getContent());
            if (item.getHashtags() != null && !item.getHashtags().isEmpty()) {
                post.setHashtags(String.join(" ", item.getHashtags()));
            }
        }

        post.setContentEditedByUser(false); // Reset cờ sửa tay khi người dùng chủ động yêu cầu tạo lại
        post.setAiProviderUsed(textProvider.getProviderName());
        post.setAiModelUsed(aiResponse.getModelUsed());
        post.setGeneratedAt(LocalDateTime.now());
        post.setStatus("READY");
        post.setGenerationError(null);

        return mapToResponse(postRepository.save(post));
    }

    public com.nqd.nqd_tool_content.dto.response.PostChatResponse chatWithAI(
            UUID userId,
            UUID postId,
            com.nqd.nqd_tool_content.dto.request.PostChatRequest chatReq
    ) {
        Post post = postRepository.findById(postId)
                .filter(p -> p.getUserId().equals(userId) && !p.getIsDeleted())
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy bài viết"));

        UserSettings settings = userSettingsRepository.findByUserId(userId).orElse(null);
        ContentPlan plan = post.getPlanId() != null ? planRepository.findById(post.getPlanId()).orElse(null) : null;

        var textProvider = aiFactory.getTextProvider(settings);
        String topic = plan != null ? plan.getTopic() : "Nội dung mạng xã hội";
        String angle = post.getAngle();
        String tone = plan != null && plan.getTone() != null ? plan.getTone() : (settings != null ? settings.getTone() : "Tự nhiên, thu hút");
        String platform = post.getPlatform();

        long startTime = System.currentTimeMillis();
        var chatRes = textProvider.chat(chatReq, topic, angle, tone, platform);
        long latencyMs = System.currentTimeMillis() - startTime;

        budgetService.recordUsage(
                userId, "TEXT", "POST_CHAT", textProvider.getProviderName(),
                chatRes.getModelUsed(), 0, 0,
                0, latencyMs, "SUCCESS", null, new java.math.BigDecimal("0.0010"), post.getId(), post.getPlanId()
        );

        return chatRes;
    }

    @Transactional
    public PostResponse regenerateImage(UUID userId, UUID postId) {
        Post post = postRepository.findById(postId)
                .filter(p -> p.getUserId().equals(userId) && !p.getIsDeleted())
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy bài viết"));

        if (!List.of("PLANNED", "READY", "GENERATION_FAILED").contains(post.getStatus())) {
            throw new BusinessException("POST_STATE_CONFLICT", "Chỉ có thể tạo lại ảnh cho bài PLANNED, READY hoặc GENERATION_FAILED");
        }

        UserSettings settings = userSettingsRepository.findByUserId(userId).orElse(null);
        ContentPlan plan = post.getPlanId() != null ? planRepository.findById(post.getPlanId()).orElse(null) : null;

        String prompt = post.getImagePrompt();
        if (prompt == null || prompt.isBlank() || prompt.startsWith("Minimalist illustration")) {
            String topic = plan != null ? plan.getTopic() : "social media";
            String angle = post.getAngle() != null ? post.getAngle() : "";
            String cleanContent = post.getContent() != null ? post.getContent().replaceAll("#\\w+", "").replaceAll("\\s+", " ").trim() : "";
            if (cleanContent.length() > 200) {
                cleanContent = cleanContent.substring(0, 200);
            }
            prompt = String.format("A professional, high quality social media photo capturing the core concept: %s. Context: %s. Clean modern aesthetic, cinematic lighting, 8k resolution, highly detailed, strictly no text, no letters, no watermark.",
                    topic + (angle.isBlank() ? "" : " - " + angle),
                    cleanContent.isBlank() ? topic : cleanContent);
            post.setImagePrompt(prompt);
        }

        var imgProvider = aiFactory.getImageProvider(settings);
        var imgReq = com.nqd.nqd_tool_content.service.ai.dto.ImageRequest.builder()
                .prompt(prompt)
                .style(plan != null ? plan.getImageStyle() : null)
                .aspectRatio("1:1")
                .build();

        long startTime = System.currentTimeMillis();
        var imgResult = imgProvider.generateImage(imgReq);
        long latencyMs = System.currentTimeMillis() - startTime;

        budgetService.recordUsage(
                userId, "IMAGE", "IMAGE_REGENERATE", imgProvider.getProviderName(),
                imgResult.getModelUsed(), 0, 0, 1, latencyMs, "SUCCESS", null,
                imgResult.getEstimatedCostUsd(), post.getId(), post.getPlanId()
        );

        if (imgResult.getImageBytes() != null && imgResult.getImageBytes().length > 0) {
            MediaAsset mediaAsset = storageService.store(
                    userId, imgResult.getImageBytes(),
                    imgResult.getMimeType() != null ? imgResult.getMimeType() : "image/png",
                    prompt, imgProvider.getProviderName(), imgResult.getModelUsed()
            );

            // Gỡ bỏ ảnh cũ nếu có
            postMediaRepository.deleteByPostId(post.getId());

            PostMedia postMedia = PostMedia.builder()
                    .postId(post.getId())
                    .mediaAssetId(mediaAsset.getId())
                    .orderIndex(0)
                    .altText(prompt)
                    .build();
            postMediaRepository.save(postMedia);
        }

        return mapToResponse(postRepository.save(post));
    }

    @Transactional
    public PostResponse uploadCustomImage(UUID userId, UUID postId, byte[] fileBytes, String mimeType, String originalFilename) {
        Post post = postRepository.findById(postId)
                .filter(p -> p.getUserId().equals(userId) && !p.getIsDeleted())
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy bài viết"));

        if (List.of("PUBLISHING", "RECONCILING", "PUBLISHED").contains(post.getStatus())) {
            throw new BusinessException("POST_STATE_CONFLICT", "Không thể thay đổi ảnh của bài viết đang hoặc đã xuất bản");
        }

        MediaAsset mediaAsset = storageService.store(
                userId, fileBytes, mimeType != null ? mimeType : "image/jpeg",
                "Uploaded: " + (originalFilename != null ? originalFilename : "custom_image"),
                "USER_UPLOAD", "MANUAL"
        );

        postMediaRepository.deleteByPostId(post.getId());

        PostMedia postMedia = PostMedia.builder()
                .postId(post.getId())
                .mediaAssetId(mediaAsset.getId())
                .orderIndex(0)
                .altText(mediaAsset.getPrompt())
                .build();
        postMediaRepository.save(postMedia);

        return mapToResponse(post);
    }

    @Transactional
    public PostResponse removeImage(UUID userId, UUID postId) {
        Post post = postRepository.findById(postId)
                .filter(p -> p.getUserId().equals(userId) && !p.getIsDeleted())
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy bài viết"));

        if (List.of("PUBLISHING", "RECONCILING", "PUBLISHED").contains(post.getStatus())) {
            throw new BusinessException("POST_STATE_CONFLICT", "Không thể gỡ ảnh của bài viết đang hoặc đã xuất bản");
        }

        postMediaRepository.deleteByPostId(post.getId());
        return mapToResponse(post);
    }

    @Transactional
    public void deletePost(UUID userId, UUID postId) {
        Post post = postRepository.findById(postId)
                .filter(p -> p.getUserId().equals(userId) && !p.getIsDeleted())
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy bài viết"));

        if (List.of("PUBLISHING", "RECONCILING").contains(post.getStatus())) {
            throw new BusinessException("POST_STATE_CONFLICT", "Không thể xóa bài viết đang được xử lý bởi hệ thống");
        }

        post.setIsDeleted(true);
        post.setDeletedAt(LocalDateTime.now());
        postRepository.save(post);
    }

    @Transactional
    public void bulkAction(UUID userId, BulkPostActionRequest req) {
        for (UUID id : req.getIds()) {
            if ("SKIP".equalsIgnoreCase(req.getAction())) {
                try {
                    skipPost(userId, id);
                } catch (Exception ignored) {}
            } else if ("DELETE".equalsIgnoreCase(req.getAction())) {
                try {
                    deletePost(userId, id);
                } catch (Exception ignored) {}
            }
        }
    }

    public List<PostAttempt> getAttempts(UUID userId, UUID postId) {
        Post post = postRepository.findById(postId)
                .filter(p -> p.getUserId().equals(userId) && !p.getIsDeleted())
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy bài viết"));
        return postAttemptRepository.findByPostIdOrderByAttemptNoAsc(post.getId());
    }

    public ScheduleSummaryResponse getScheduleSummary(UUID userId) {
        List<Post> posts = postRepository.findByUserIdAndIsDeletedFalseOrderByScheduledAtAsc(userId);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endOfToday = now.toLocalDate().atTime(23, 59, 59);
        LocalDateTime next24h = now.plusHours(24);

        long publishedToday = posts.stream()
                .filter(p -> "PUBLISHED".equalsIgnoreCase(p.getStatus()))
                .filter(p -> p.getPublishedAt() != null && !p.getPublishedAt().isAfter(endOfToday) && !p.getPublishedAt().isBefore(now.toLocalDate().atStartOfDay()))
                .count();

        long scheduledNext24h = posts.stream()
                .filter(p -> List.of("PLANNED", "READY").contains(p.getStatus()))
                .filter(p -> p.getScheduledAt() != null && !p.getScheduledAt().isBefore(now) && !p.getScheduledAt().isAfter(next24h))
                .count();

        long needsAttention = posts.stream()
                .filter(p -> List.of("FAILED", "NEEDS_REVIEW", "GENERATION_FAILED", "MISSED").contains(p.getStatus()))
                .count();

        return ScheduleSummaryResponse.builder()
                .publishedToday(publishedToday)
                .scheduledNext24h(scheduledNext24h)
                .needsAttention(needsAttention)
                .build();
    }

    private PostResponse mapToResponse(Post p) {
        String planName = null;
        if (p.getPlanId() != null) {
            planName = planRepository.findById(p.getPlanId()).map(ContentPlan::getName).orElse(null);
        }

        String channelName = null;
        if (p.getSocialAccountId() != null) {
            channelName = accountRepository.findById(p.getSocialAccountId()).map(SocialAccount::getDisplayName).orElse(null);
        }

        List<String> mediaUrls = new ArrayList<>();
        List<PostMedia> postMedias = postMediaRepository.findByPostIdOrderByOrderIndexAsc(p.getId());
        for (PostMedia pm : postMedias) {
            mediaAssetRepository.findById(pm.getMediaAssetId()).ifPresent(ma -> {
                String url = storageService.getPublicUrl(ma.getPublicToken());
                if (url != null) mediaUrls.add(url);
            });
        }

        return PostResponse.builder()
                .id(p.getId().toString())
                .planId(p.getPlanId() != null ? p.getPlanId().toString() : null)
                .planName(planName)
                .source(p.getSource())
                .groupId(p.getGroupId() != null ? p.getGroupId().toString() : null)
                .slotKey(p.getSlotKey())
                .socialAccountId(p.getSocialAccountId().toString())
                .channelName(channelName)
                .platform(p.getPlatform())
                .scheduledAt(p.getScheduledAt())
                .scheduledTimezone(p.getScheduledTimezone())
                .generateAt(p.getGenerateAt())
                .status(p.getStatus())
                .content(p.getContent())
                .hashtags(p.getHashtags())
                .threadParts(p.getThreadParts())
                .imagePrompt(p.getImagePrompt())
                .contentEditedByUser(p.getContentEditedByUser())
                .angle(p.getAngle())
                .publishCycle(p.getPublishCycle())
                .attemptCount(p.getAttemptCount())
                .maxAttempts(p.getMaxAttempts())
                .nextAttemptAt(p.getNextAttemptAt())
                .errorClass(p.getErrorClass())
                .lastError(p.getLastError())
                .needsReviewReason(p.getNeedsReviewReason())
                .platformPostId(p.getPlatformPostId())
                .platformPostUrl(p.getPlatformPostUrl())
                .publishedAt(p.getPublishedAt())
                .mediaUrls(mediaUrls)
                .build();
    }
}
