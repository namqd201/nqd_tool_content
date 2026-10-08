package com.nqd.nqd_tool_content.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "posts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "plan_id")
    private UUID planId;

    @Builder.Default
    @Column(name = "source", nullable = false, length = 30)
    private String source = "PLAN"; // PLAN, MANUAL

    @Column(name = "group_id")
    private UUID groupId;

    @Column(name = "slot_key", nullable = false, length = 100)
    private String slotKey;

    @Column(name = "social_account_id", nullable = false)
    private UUID socialAccountId;

    @Column(name = "platform", nullable = false, length = 50)
    private String platform;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @Builder.Default
    @Column(name = "scheduled_timezone", nullable = false, length = 50)
    private String scheduledTimezone = "Asia/Ho_Chi_Minh";

    @Column(name = "generate_at")
    private LocalDateTime generateAt;

    @Builder.Default
    @Column(name = "missed_grace_minutes")
    private Integer missedGraceMinutes = 120;

    @Builder.Default
    @Column(name = "status", nullable = false, length = 30)
    private String status = "PLANNED"; // PLANNED, GENERATING, READY, PUBLISHING, PUBLISHED, FAILED, NEEDS_REVIEW, RECONCILING, SKIPPED

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "hashtags", columnDefinition = "TEXT")
    private String hashtags;

    @Column(name = "thread_parts", columnDefinition = "TEXT")
    private String threadParts;

    @Column(name = "image_prompt", columnDefinition = "TEXT")
    private String imagePrompt;

    @Builder.Default
    @Column(name = "content_edited_by_user")
    private Boolean contentEditedByUser = false;

    @Column(name = "angle", columnDefinition = "TEXT")
    private String angle;

    @Builder.Default
    @Column(name = "generation_attempts")
    private Integer generationAttempts = 0;

    @Column(name = "generation_error", columnDefinition = "TEXT")
    private String generationError;

    @Column(name = "ai_provider_used", length = 50)
    private String aiProviderUsed;

    @Column(name = "ai_model_used", length = 50)
    private String aiModelUsed;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    @Builder.Default
    @Column(name = "publish_cycle", nullable = false)
    private Integer publishCycle = 1;

    @Builder.Default
    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount = 0;

    @Builder.Default
    @Column(name = "max_attempts", nullable = false)
    private Integer maxAttempts = 3;

    @Column(name = "next_attempt_at")
    private LocalDateTime nextAttemptAt;

    @Column(name = "locked_by", length = 100)
    private String lockedBy;

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    @Column(name = "lease_expires_at")
    private LocalDateTime leaseExpiresAt;

    @Column(name = "request_sent_at")
    private LocalDateTime requestSentAt;

    @Builder.Default
    @Column(name = "reconcile_count", nullable = false)
    private Integer reconcileCount = 0;

    @Builder.Default
    @Column(name = "error_class", length = 50)
    private String errorClass = "NONE";

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "needs_review_reason", columnDefinition = "TEXT")
    private String needsReviewReason;

    @Column(name = "progress", columnDefinition = "TEXT")
    private String progress;

    @Column(name = "platform_post_id", length = 255)
    private String platformPostId;

    @Column(name = "platform_post_url", length = 1000)
    private String platformPostUrl;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;
}
