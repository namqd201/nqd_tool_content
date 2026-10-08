package com.nqd.nqd_tool_content.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_usage_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiUsageLog extends BaseIdEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Builder.Default
    @Column(name = "kind", nullable = false, length = 30)
    private String kind = "TEXT"; // TEXT, IMAGE

    @Column(name = "feature", nullable = false, length = 50)
    private String feature; // PLAN_ANGLES, POST_GENERATION, IMAGE_GENERATION, SAMPLE_PREVIEW

    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    @Column(name = "model", length = 50)
    private String model;

    @Builder.Default
    @Column(name = "prompt_tokens")
    private Integer promptTokens = 0;

    @Builder.Default
    @Column(name = "completion_tokens")
    private Integer completionTokens = 0;

    @Builder.Default
    @Column(name = "image_count")
    private Integer imageCount = 0;

    @Builder.Default
    @Column(name = "latency_ms")
    private Long latencyMs = 0L;

    @Builder.Default
    @Column(name = "status", nullable = false, length = 30)
    private String status = "SUCCESS";

    @Column(name = "error", columnDefinition = "TEXT")
    private String error;

    @Builder.Default
    @Column(name = "estimated_cost_usd", precision = 10, scale = 4)
    private BigDecimal estimatedCostUsd = BigDecimal.ZERO;

    @Column(name = "post_id")
    private UUID postId;

    @Column(name = "plan_id")
    private UUID planId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
