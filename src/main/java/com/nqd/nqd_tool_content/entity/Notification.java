package com.nqd.nqd_tool_content.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "type", nullable = false, length = 50)
    private String type; // POST_PUBLISHED, POST_FAILED, POST_NEEDS_REVIEW, POST_MISSED, GENERATION_FAILED, AI_BUDGET_EXCEEDED, TOKEN_EXPIRING, TOKEN_EXPIRED, PLAN_COMPLETED, DAILY_DIGEST, SYSTEM

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload; // JSON data

    @Column(name = "related_post_id")
    private UUID relatedPostId;

    @Column(name = "related_plan_id")
    private UUID relatedPlanId;

    @Builder.Default
    @Column(name = "delivery_status", length = 30)
    private String deliveryStatus = "PENDING"; // PENDING, SENT, FAILED

    @Column(name = "channels_sent")
    private String channelsSent; // JSON array or comma separated, e.g. "TELEGRAM,EMAIL"

    @Builder.Default
    @Column(name = "delivery_attempts")
    private Integer deliveryAttempts = 0;

    @Column(name = "next_delivery_at")
    private LocalDateTime nextDeliveryAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;
}
