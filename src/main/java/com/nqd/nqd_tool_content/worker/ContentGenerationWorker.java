package com.nqd.nqd_tool_content.worker;

import com.nqd.nqd_tool_content.entity.*;
import com.nqd.nqd_tool_content.repository.*;
import com.nqd.nqd_tool_content.service.NotificationService;
import com.nqd.nqd_tool_content.service.ai.AIProvider;
import com.nqd.nqd_tool_content.service.ai.BudgetService;
import com.nqd.nqd_tool_content.service.ai.ContentGuard;
import com.nqd.nqd_tool_content.service.ai.ImageProvider;
import com.nqd.nqd_tool_content.service.ai.dto.*;
import com.nqd.nqd_tool_content.service.ai.impl.AIFactory;
import com.nqd.nqd_tool_content.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContentGenerationWorker {

    private final PostRepository postRepository;
    private final ContentPlanRepository planRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final PostMediaRepository postMediaRepository;
    private final AIFactory aiFactory;
    private final ContentGuard contentGuard;
    private final BudgetService budgetService;
    private final StorageService storageService;
    private final NotificationService notificationService;

    private static final String WORKER_ID = "content-gen-worker-1";
    private volatile LocalDateTime lastTickAt = LocalDateTime.now();

    public LocalDateTime getLastTickAt() {
        return lastTickAt;
    }

    public int triggerNowForUser(UUID userId) {
        List<Post> posts = postRepository.findByUserIdAndStatusAndIsDeletedFalse(userId, "PLANNED");
        if (posts.isEmpty()) return 0;
        LocalDateTime now = LocalDateTime.now();
        for (Post p : posts) {
            p.setGenerateAt(now.minusMinutes(1));
            postRepository.save(p);
        }
        new Thread(this::runGenerationCycle).start();
        return posts.size();
    }

    /**
     * Polls every 30 seconds for posts in PLANNED status whose generate_at has passed.
     */
    @Scheduled(fixedDelay = 30000)
    public void runGenerationCycle() {
        this.lastTickAt = LocalDateTime.now();
        LocalDateTime now = LocalDateTime.now();
        List<Post> plannedPosts = postRepository
                .findByStatusAndGenerateAtLessThanEqualAndIsDeletedFalseOrderByGenerateAtAsc("PLANNED", now);

        if (plannedPosts.isEmpty()) {
            return;
        }

        log.info("[ContentGenerationWorker] Found {} PLANNED posts to generate content", plannedPosts.size());

        // Group by group_id (or post id if groupId is null)
        Map<UUID, List<Post>> grouped = plannedPosts.stream()
                .collect(Collectors.groupingBy(p -> p.getGroupId() != null ? p.getGroupId() : p.getId()));

        for (Map.Entry<UUID, List<Post>> entry : grouped.entrySet()) {
            try {
                processGroup(entry.getKey(), entry.getValue());
            } catch (Exception e) {
                log.error("[ContentGenerationWorker] Error processing group {}: {}", entry.getKey(), e.getMessage(), e);
            }
        }
    }

    @Transactional
    public void processGroup(UUID groupId, List<Post> posts) {
        if (posts.isEmpty()) return;

        Post primaryPost = posts.get(0);
        UUID userId = primaryPost.getUserId();
        UUID planId = primaryPost.getPlanId();

        ContentPlan plan = (planId != null) ? planRepository.findById(planId).orElse(null) : null;
        UserSettings settings = userSettingsRepository.findByUserId(userId).orElse(null);

        // 1. Mark status = GENERATING (atomic lock lease)
        LocalDateTime leaseExpires = LocalDateTime.now().plusMinutes(5);
        for (Post p : posts) {
            p.setStatus("GENERATING");
            p.setLockedBy(WORKER_ID);
            p.setLockedAt(LocalDateTime.now());
            p.setLeaseExpiresAt(leaseExpires);
            p.setGenerationAttempts((p.getGenerationAttempts() != null ? p.getGenerationAttempts() : 0) + 1);
            postRepository.save(p);
        }

        // 2. Budget Check
        BigDecimal estimatedCost = new BigDecimal("0.0150"); // Approx cost for text + image
        if (!budgetService.hasRemainingBudget(userId, estimatedCost)) {
            log.warn("[ContentGenerationWorker] Group {} paused: daily budget exceeded for user {}", groupId, userId);
            for (Post p : posts) {
                p.setStatus("PLANNED");
                p.setLockedBy(null);
                p.setLeaseExpiresAt(null);
                p.setGenerationError("AI daily budget exceeded");
                postRepository.save(p);
            }
            return;
        }

        // 3. Prepare AI Request
        List<String> platforms = posts.stream().map(Post::getPlatform).distinct().toList();
        List<String> forbiddenWords = new ArrayList<>();
        if (settings != null && settings.getForbiddenWords() != null) {
            forbiddenWords.addAll(Arrays.asList(settings.getForbiddenWords().split("[,;]")));
        }

        List<String> preferredHashtags = new ArrayList<>();
        if (settings != null && settings.getPreferredHashtags() != null) {
            preferredHashtags.addAll(Arrays.asList(settings.getPreferredHashtags().split("[\\s,;]+")));
        }

        boolean includeImage = (plan != null && Boolean.TRUE.equals(plan.getIncludeImage()));

        List<String> recentPosts = new ArrayList<>();
        if (planId != null) {
            List<Post> previousPlanPosts = postRepository.findByPlanIdAndIsDeletedFalseOrderByScheduledAtAsc(planId);
            for (Post prev : previousPlanPosts) {
                if (prev.getContent() != null && !prev.getContent().isBlank() && !prev.getId().equals(primaryPost.getId())) {
                    String cleanSnippet = prev.getContent().replaceAll("#\\w+", "").replaceAll("\\s+", " ").trim();
                    if (cleanSnippet.length() > 140) {
                        cleanSnippet = cleanSnippet.substring(0, 140) + "...";
                    }
                    recentPosts.add((prev.getAngle() != null ? "[" + prev.getAngle() + "] " : "") + cleanSnippet);
                    if (recentPosts.size() >= 5) break;
                }
            }
        }

        AIContentRequest req = AIContentRequest.builder()
                .topic(plan != null ? plan.getTopic() : "Bài viết chia sẻ kiến thức")
                .angle(primaryPost.getAngle())
                .instructions(plan != null ? plan.getInstructions() : "")
                .tone(plan != null && plan.getTone() != null ? plan.getTone() : (settings != null ? settings.getTone() : null))
                .language(plan != null && plan.getLanguage() != null ? plan.getLanguage() : "vi")
                .platforms(platforms)
                .forbiddenWords(forbiddenWords)
                .preferredHashtags(preferredHashtags)
                .recentPosts(recentPosts)
                .generateImagePrompt(includeImage)
                .build();

        AIProvider textProvider = aiFactory.getTextProvider(settings);
        long startTime = System.currentTimeMillis();

        AIContentResponse aiResponse;
        try {
            aiResponse = textProvider.generateContent(req);
        } catch (Exception e) {
            log.error("[ContentGenerationWorker] AI generation failed for group {}: {}", groupId, e.getMessage());
            handleGenerationFailure(posts, e.getMessage());
            return;
        }

        long latencyMs = System.currentTimeMillis() - startTime;

        // Record AI Usage
        budgetService.recordUsage(
                userId,
                "TEXT",
                "POST_GENERATION",
                textProvider.getProviderName(),
                aiResponse.getModelUsed(),
                aiResponse.getPromptTokens(),
                aiResponse.getCompletionTokens(),
                0,
                latencyMs,
                "SUCCESS",
                null,
                aiResponse.getEstimatedCostUsd(),
                primaryPost.getId(),
                planId
        );

        // 4. Validate output with ContentGuard
        Map<String, GeneratedPlatformPost> generatedByPlatform = new HashMap<>();
        if (aiResponse.getPosts() != null) {
            for (GeneratedPlatformPost item : aiResponse.getPosts()) {
                generatedByPlatform.put(item.getPlatform().toUpperCase(), item);
            }
        }

        for (Post p : posts) {
            if (Boolean.TRUE.equals(p.getContentEditedByUser())) {
                continue; // Preserve manual edits
            }

            GeneratedPlatformPost item = generatedByPlatform.get(p.getPlatform().toUpperCase());
            String text = (item != null && item.getContent() != null) ? item.getContent() : "";

            GuardResult guard = contentGuard.checkContent(p.getPlatform(), text, forbiddenWords, Collections.emptyList());
            if (!guard.isAllowed()) {
                String errorReason = String.join("; ", guard.getErrors());
                log.warn("[ContentGenerationWorker] ContentGuard rejected post {} for {}: {}", p.getId(), p.getPlatform(), errorReason);
                handleGenerationFailure(posts, "ContentGuard: " + errorReason);
                return;
            }

            p.setContent(text);
            if (item != null && item.getHashtags() != null && !item.getHashtags().isEmpty()) {
                p.setHashtags(String.join(" ", item.getHashtags()));
            }
            if (item != null && item.getThreadParts() != null && !item.getThreadParts().isEmpty()) {
                p.setThreadParts(String.join("\n---\n", item.getThreadParts()));
            }
            p.setImagePrompt(aiResponse.getSuggestedImagePrompt());
            p.setAiProviderUsed(textProvider.getProviderName());
            p.setAiModelUsed(aiResponse.getModelUsed());
            p.setGeneratedAt(LocalDateTime.now());
        }

        // 5. Generate Image if configured
        MediaAsset mediaAsset = null;
        if (includeImage && aiResponse.getSuggestedImagePrompt() != null && !aiResponse.getSuggestedImagePrompt().isBlank()) {
            ImageProvider imgProvider = aiFactory.getImageProvider(settings);
            ImageRequest imgReq = ImageRequest.builder()
                    .prompt(aiResponse.getSuggestedImagePrompt())
                    .style(plan != null ? plan.getImageStyle() : null)
                    .aspectRatio("1:1")
                    .build();

            long imgStart = System.currentTimeMillis();
            try {
                ImageResult imgResult = imgProvider.generateImage(imgReq);
                long imgLatency = System.currentTimeMillis() - imgStart;

                budgetService.recordUsage(
                        userId,
                        "IMAGE",
                        "IMAGE_GENERATION",
                        imgProvider.getProviderName(),
                        imgResult.getModelUsed(),
                        0,
                        0,
                        1,
                        imgLatency,
                        "SUCCESS",
                        null,
                        imgResult.getEstimatedCostUsd(),
                        primaryPost.getId(),
                        planId
                );

                if (imgResult.getImageBytes() != null && imgResult.getImageBytes().length > 0) {
                    mediaAsset = storageService.store(
                            userId,
                            imgResult.getImageBytes(),
                            imgResult.getMimeType() != null ? imgResult.getMimeType() : "image/png",
                            aiResponse.getSuggestedImagePrompt(),
                            imgProvider.getProviderName(),
                            imgResult.getModelUsed()
                    );
                }
            } catch (Exception e) {
                log.error("[ContentGenerationWorker] Image generation failed for group {}: {}", groupId, e.getMessage());
                String failurePolicy = (plan != null && plan.getImageFailurePolicy() != null)
                        ? plan.getImageFailurePolicy()
                        : "POST_WITHOUT_IMAGE";

                if ("FAIL".equalsIgnoreCase(failurePolicy)) {
                    handleGenerationFailure(posts, "Image generation failed: " + e.getMessage());
                    return;
                } else if ("SKIP_POST".equalsIgnoreCase(failurePolicy)) {
                    for (Post p : posts) {
                        p.setStatus("SKIPPED");
                        p.setLastError("Skipped due to image failure policy");
                        postRepository.save(p);
                    }
                    return;
                }
                // POST_WITHOUT_IMAGE continues
            }
        }

        // 6. Finalize posts to READY
        for (Post p : posts) {
            p.setStatus("READY");
            p.setNextAttemptAt(p.getScheduledAt());
            p.setLockedBy(null);
            p.setLeaseExpiresAt(null);
            p.setGenerationError(null);
            postRepository.save(p);

            // Link media asset
            if (mediaAsset != null) {
                PostMedia postMedia = PostMedia.builder()
                        .postId(p.getId())
                        .mediaAssetId(mediaAsset.getId())
                        .orderIndex(0)
                        .altText(mediaAsset.getPrompt())
                        .build();
                postMediaRepository.save(postMedia);
            }
        }

        log.info("[ContentGenerationWorker] Successfully generated content for group {} ({} posts) -> READY", groupId, posts.size());
    }

    private void handleGenerationFailure(List<Post> posts, String errorMsg) {
        LocalDateTime now = LocalDateTime.now();
        for (Post p : posts) {
            int attempts = p.getGenerationAttempts() != null ? p.getGenerationAttempts() : 1;
            boolean hasTime = (p.getScheduledAt() != null && now.plusMinutes(30).isBefore(p.getScheduledAt()));

            if (attempts < 3 && hasTime) {
                // Temporary retry backoff (1m, 5m, 15m)
                int backoffMinutes = attempts == 1 ? 1 : (attempts == 2 ? 5 : 15);
                p.setStatus("PLANNED");
                p.setGenerateAt(now.plusMinutes(backoffMinutes));
                p.setGenerationError(errorMsg);
                p.setLockedBy(null);
                p.setLeaseExpiresAt(null);
            } else {
                p.setStatus("GENERATION_FAILED");
                p.setGenerationError(errorMsg);
                p.setLockedBy(null);
                p.setLeaseExpiresAt(null);

                // Notify user
                notificationService.enqueue(
                        p.getUserId(),
                        "GENERATION_FAILED",
                        "Lỗi sinh nội dung AI",
                        "Không thể tự động sinh nội dung cho bài đăng lúc " + p.getScheduledAt() + ". Lý do: " + errorMsg,
                        p.getId(),
                        p.getPlanId()
                );
            }
            postRepository.save(p);
        }
    }
}
