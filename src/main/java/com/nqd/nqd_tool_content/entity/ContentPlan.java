package com.nqd.nqd_tool_content.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "content_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentPlan extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "topic", columnDefinition = "TEXT", nullable = false)
    private String topic;

    @Column(name = "instructions", columnDefinition = "TEXT")
    private String instructions;

    @Builder.Default
    @Column(name = "language", length = 10)
    private String language = "vi";

    @Column(name = "tone", length = 100)
    private String tone;

    @Builder.Default
    @Column(name = "status", nullable = false, length = 30)
    private String status = "DRAFT"; // DRAFT, ACTIVE, PAUSED, COMPLETED, CANCELLED

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Builder.Default
    @Column(name = "timezone", nullable = false, length = 50)
    private String timezone = "Asia/Ho_Chi_Minh";

    @Builder.Default
    @Column(name = "posts_per_day", nullable = false)
    private Integer postsPerDay = 1;

    @Builder.Default
    @Column(name = "time_mode", nullable = false, length = 30)
    private String timeMode = "FIXED_TIMES"; // FIXED_TIMES, WINDOWS

    @Column(name = "time_slots", columnDefinition = "TEXT")
    private String timeSlots; // JSON array of string times e.g. ["09:00", "15:00"]

    @Builder.Default
    @Column(name = "min_gap_minutes")
    private Integer minGapMinutes = 60;

    @Builder.Default
    @Column(name = "jitter_minutes")
    private Integer jitterMinutes = 0;

    @Builder.Default
    @Column(name = "content_mode", length = 30)
    private String contentMode = "ADAPT_SAME_IDEA"; // ADAPT_SAME_IDEA, INDEPENDENT

    @Builder.Default
    @Column(name = "include_image")
    private Boolean includeImage = true;

    @Column(name = "image_style", columnDefinition = "TEXT")
    private String imageStyle;

    @Builder.Default
    @Column(name = "image_failure_policy", length = 30)
    private String imageFailurePolicy = "POST_WITHOUT_IMAGE"; // POST_WITHOUT_IMAGE, SKIP_POST, FAIL

    @Builder.Default
    @Column(name = "generate_lead_hours")
    private Integer generateLeadHours = 12;

    @Builder.Default
    @Column(name = "missed_grace_minutes")
    private Integer missedGraceMinutes = 120;

    @Column(name = "angles", columnDefinition = "TEXT")
    private String angles; // JSON array of topic angles

    @Builder.Default
    @Column(name = "estimated_cost_usd", precision = 8, scale = 2)
    private BigDecimal estimatedCostUsd = BigDecimal.ZERO;

    @Column(name = "activated_at")
    private LocalDateTime activatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
