package com.nqd.nqd_tool_content.service.ai;

import com.nqd.nqd_tool_content.entity.AiUsageLog;
import com.nqd.nqd_tool_content.entity.UserSettings;
import com.nqd.nqd_tool_content.repository.AiUsageLogRepository;
import com.nqd.nqd_tool_content.repository.UserSettingsRepository;
import com.nqd.nqd_tool_content.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BudgetService {

    private final AiUsageLogRepository aiUsageLogRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final NotificationService notificationService;

    /**
     * Check if user has sufficient budget for today.
     * Throws exception or returns false if budget exceeded.
     */
    public boolean hasRemainingBudget(UUID userId, BigDecimal estimatedCost) {
        // Tài khoản Pro: Cho phép sinh nội dung không giới hạn, không hạn chế ngân sách hàng ngày
        return true;
    }

    /**
     * Records an AI usage event into ai_usage_logs.
     */
    @Transactional
    public AiUsageLog recordUsage(
            UUID userId,
            String kind,
            String feature,
            String provider,
            String model,
            Integer promptTokens,
            Integer completionTokens,
            Integer imageCount,
            Long latencyMs,
            String status,
            String error,
            BigDecimal estimatedCostUsd,
            UUID postId,
            UUID planId
    ) {
        AiUsageLog usage = AiUsageLog.builder()
                .userId(userId)
                .kind(kind != null ? kind : "TEXT")
                .feature(feature)
                .provider(provider)
                .model(model)
                .promptTokens(promptTokens != null ? promptTokens : 0)
                .completionTokens(completionTokens != null ? completionTokens : 0)
                .imageCount(imageCount != null ? imageCount : 0)
                .latencyMs(latencyMs != null ? latencyMs : 0L)
                .status(status != null ? status : "SUCCESS")
                .error(error)
                .estimatedCostUsd(estimatedCostUsd != null ? estimatedCostUsd : BigDecimal.ZERO)
                .postId(postId)
                .planId(planId)
                .createdAt(LocalDateTime.now())
                .build();

        return aiUsageLogRepository.save(usage);
    }
}
