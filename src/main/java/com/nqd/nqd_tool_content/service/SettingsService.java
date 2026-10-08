package com.nqd.nqd_tool_content.service;

import com.nqd.nqd_tool_content.dto.response.AiPipelineProgressResponse;
import com.nqd.nqd_tool_content.entity.AiUsageLog;
import com.nqd.nqd_tool_content.entity.ContentPlan;
import com.nqd.nqd_tool_content.entity.Post;
import com.nqd.nqd_tool_content.entity.UserSettings;
import com.nqd.nqd_tool_content.repository.AiUsageLogRepository;
import com.nqd.nqd_tool_content.repository.ContentPlanRepository;
import com.nqd.nqd_tool_content.repository.PostRepository;
import com.nqd.nqd_tool_content.repository.UserSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettingsService {

    private final UserSettingsRepository userSettingsRepository;
    private final PostRepository postRepository;
    private final ContentPlanRepository contentPlanRepository;
    private final AiUsageLogRepository aiUsageLogRepository;

    @Transactional
    public UserSettings getOrCreateSettings(UUID userId) {
        return userSettingsRepository.findByUserId(userId).orElseGet(() -> {
            UserSettings settings = UserSettings.builder()
                    .userId(userId)
                    .timezone("Asia/Ho_Chi_Minh")
                    .defaultLanguage("vi")
                    .tone("Chuyên nghiệp, truyền cảm hứng, ngắn gọn")
                    .writingStyle("Súc tích, gãy gọn, tập trung vào giá trị thực tiễn.")
                    .emojiPolicy("MODERATE")
                    .textProviderPrimary("GEMINI")
                    .textProviderFallback("OPENAI")
                    .imageProviderPrimary("GEMINI_IMAGE")
                    .imageProviderFallback("OPENAI_IMAGE")
                    .aiDailyBudgetUsd(new BigDecimal("5.00"))
                    .generateLeadHours(12)
                    .missedGraceMinutes(120)
                    .build();
            return userSettingsRepository.save(settings);
        });
    }

    @Transactional
    public UserSettings updateSettings(UUID userId, UserSettings newSettings) {
        UserSettings current = getOrCreateSettings(userId);

        // Validate timezone
        if (newSettings.getTimezone() != null) {
            try {
                ZoneId.of(newSettings.getTimezone());
                current.setTimezone(newSettings.getTimezone());
            } catch (Exception e) {
                throw new IllegalArgumentException("Múi giờ IANA không hợp lệ: " + newSettings.getTimezone());
            }
        }

        if (newSettings.getDefaultLanguage() != null) current.setDefaultLanguage(newSettings.getDefaultLanguage());
        if (newSettings.getTone() != null) current.setTone(newSettings.getTone());
        if (newSettings.getWritingStyle() != null) current.setWritingStyle(newSettings.getWritingStyle());
        if (newSettings.getForbiddenWords() != null) current.setForbiddenWords(newSettings.getForbiddenWords());
        if (newSettings.getPreferredHashtags() != null) current.setPreferredHashtags(newSettings.getPreferredHashtags());
        if (newSettings.getDefaultCta() != null) current.setDefaultCta(newSettings.getDefaultCta());
        if (newSettings.getEmojiPolicy() != null) current.setEmojiPolicy(newSettings.getEmojiPolicy());
        if (newSettings.getExtraGuidelines() != null) current.setExtraGuidelines(newSettings.getExtraGuidelines());

        // AI configuration
        if (newSettings.getTextProviderPrimary() != null) current.setTextProviderPrimary(newSettings.getTextProviderPrimary());
        if (newSettings.getTextProviderFallback() != null) current.setTextProviderFallback(newSettings.getTextProviderFallback());
        if (newSettings.getImageProviderPrimary() != null) current.setImageProviderPrimary(newSettings.getImageProviderPrimary());
        if (newSettings.getImageProviderFallback() != null) current.setImageProviderFallback(newSettings.getImageProviderFallback());

        if (newSettings.getAiDailyBudgetUsd() != null) {
            if (newSettings.getAiDailyBudgetUsd().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Ngân sách AI/ngày không được nhỏ hơn 0");
            }
            current.setAiDailyBudgetUsd(newSettings.getAiDailyBudgetUsd());
        }

        if (newSettings.getAppendAiDisclosure() != null) current.setAppendAiDisclosure(newSettings.getAppendAiDisclosure());
        if (newSettings.getAiDisclosureText() != null) current.setAiDisclosureText(newSettings.getAiDisclosureText());

        // Notifications
        if (newSettings.getNotifyTelegramChatId() != null) current.setNotifyTelegramChatId(newSettings.getNotifyTelegramChatId());
        if (newSettings.getNotifyEmail() != null) current.setNotifyEmail(newSettings.getNotifyEmail());
        if (newSettings.getNotifyEvents() != null) current.setNotifyEvents(newSettings.getNotifyEvents());
        if (newSettings.getDailyDigestEnabled() != null) current.setDailyDigestEnabled(newSettings.getDailyDigestEnabled());
        if (newSettings.getDailyDigestTime() != null) current.setDailyDigestTime(newSettings.getDailyDigestTime());

        // Operations
        if (newSettings.getGenerateLeadHours() != null) current.setGenerateLeadHours(newSettings.getGenerateLeadHours());
        if (newSettings.getMissedGraceMinutes() != null) current.setMissedGraceMinutes(newSettings.getMissedGraceMinutes());

        return userSettingsRepository.save(current);
    }

    public List<Map<String, Object>> getAvailableAiProviders() {
        return List.of(
                Map.of("id", "GEMINI", "name", "Google Gemini (3.8 Flash / Pro)", "type", "TEXT", "available", true),
                Map.of("id", "OPENAI", "name", "OpenAI (GPT-4o / GPT-4o-mini)", "type", "TEXT", "available", true),
                Map.of("id", "ANTHROPIC", "name", "Anthropic Claude (3.7 Sonnet)", "type", "TEXT", "available", true),
                Map.of("id", "GEMINI_IMAGE", "name", "Google Imagen 3", "type", "IMAGE", "available", true),
                Map.of("id", "OPENAI_IMAGE", "name", "OpenAI DALL-E 3", "type", "IMAGE", "available", true)
        );
    }

    @Transactional(readOnly = true)
    public AiPipelineProgressResponse getAiPipelineProgress(UUID userId) {
        long planned = postRepository.countByUserIdAndStatusAndIsDeletedFalse(userId, "PLANNED");
        long generating = postRepository.countByUserIdAndStatusAndIsDeletedFalse(userId, "GENERATING");
        long ready = postRepository.countByUserIdAndStatusAndIsDeletedFalse(userId, "READY");
        long failed = postRepository.countByUserIdAndStatusAndIsDeletedFalse(userId, "GENERATION_FAILED");

        // Lấy danh sách các bài đang hoặc vừa xử lý gần nhất
        List<Post> candidatePosts = postRepository.findTop20ByUserIdAndStatusInOrderByUpdatedAtDesc(
                userId, List.of("GENERATING", "PLANNED", "READY", "GENERATION_FAILED")
        );

        // Lấy plans cache map
        Set<UUID> planIds = candidatePosts.stream()
                .map(Post::getPlanId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, ContentPlan> planMap = contentPlanRepository.findAllById(planIds).stream()
                .collect(Collectors.toMap(ContentPlan::getId, p -> p));

        List<AiPipelineProgressResponse.ActivePipelineTask> tasks = new ArrayList<>();
        for (Post p : candidatePosts) {
            ContentPlan plan = p.getPlanId() != null ? planMap.get(p.getPlanId()) : null;
            String planName = plan != null ? plan.getName() : "Bài viết thủ công";
            String topic = plan != null ? plan.getTopic() : "Chủ đề tự động";

            int step = 1;
            String stepName = "Khám phá chủ đề & Góc nhìn (Research)";
            String desc = "Đang phân tích chủ đề, tìm kiếm góc tiếp cận độc đáo";
            int percent = 20;

            if ("GENERATING".equalsIgnoreCase(p.getStatus())) {
                step = 3;
                stepName = "Sinh nội dung đa kênh (Content Generation)";
                desc = "Đang gọi Gemini 3.8 Flash sinh bài viết chuẩn phong cách thương hiệu";
                percent = 60;
            } else if ("READY".equalsIgnoreCase(p.getStatus())) {
                step = 5;
                stepName = "Hoàn tất & Sẵn sàng đăng (Ready to publish)";
                desc = "Đã kiểm duyệt an toàn, sinh ảnh minh họa và lên lịch đăng chuẩn giờ";
                percent = 100;
            } else if ("GENERATION_FAILED".equalsIgnoreCase(p.getStatus())) {
                step = 4;
                stepName = "Gặp lỗi sinh bài (Error)";
                desc = p.getGenerationError() != null ? p.getGenerationError() : "Lỗi không xác định khi gọi AI";
                percent = 70;
            } else {
                // PLANNED
                step = 1;
                stepName = "Chờ hàng đợi sinh nội dung (In Queue)";
                desc = "Đang trong hàng đợi điều phối tự động";
                percent = 15;
            }

            tasks.add(AiPipelineProgressResponse.ActivePipelineTask.builder()
                    .postId(p.getId())
                    .planId(p.getPlanId())
                    .planName(planName)
                    .platform(p.getPlatform())
                    .topic(topic)
                    .status(p.getStatus())
                    .currentStep(step)
                    .currentStepName(stepName)
                    .progressDescription(desc)
                    .percentComplete(percent)
                    .modelUsed(p.getAiModelUsed() != null ? p.getAiModelUsed() : "gemini-3.8-flash")
                    .updatedAt(p.getUpdatedAt() != null ? p.getUpdatedAt() : p.getCreatedAt())
                    .build());
        }

        // Lấy lịch sử hoạt động AI gần nhất
        List<AiUsageLog> logs = aiUsageLogRepository.findTop50ByUserIdOrderByCreatedAtDesc(userId);
        List<AiPipelineProgressResponse.AiRecentActivity> activities = logs.stream()
                .map(l -> AiPipelineProgressResponse.AiRecentActivity.builder()
                        .id(l.getId())
                        .kind(l.getKind())
                        .feature(l.getFeature())
                        .provider(l.getProvider())
                        .model(l.getModel())
                        .promptTokens(l.getPromptTokens() != null ? l.getPromptTokens() : 0)
                        .completionTokens(l.getCompletionTokens() != null ? l.getCompletionTokens() : 0)
                        .latencyMs(l.getLatencyMs() != null ? l.getLatencyMs() : 0L)
                        .status(l.getStatus())
                        .error(l.getError())
                        .createdAt(l.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return AiPipelineProgressResponse.builder()
                .totalPlanned(planned)
                .totalGenerating(generating)
                .totalReady(ready)
                .totalFailed(failed)
                .activeTasks(tasks)
                .recentActivities(activities)
                .build();
    }
}
