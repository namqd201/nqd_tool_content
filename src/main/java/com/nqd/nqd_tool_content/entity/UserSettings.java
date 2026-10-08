package com.nqd.nqd_tool_content.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "user_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSettings extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Builder.Default
    @Column(name = "timezone", length = 50)
    private String timezone = "Asia/Ho_Chi_Minh";

    @Builder.Default
    @Column(name = "default_language", length = 10)
    private String defaultLanguage = "vi";

    // Persona & Brand Voice
    @Builder.Default
    @Column(name = "tone", length = 100)
    private String tone = "Chuyên nghiệp, truyền cảm hứng, ngắn gọn";

    @Column(name = "writing_style", columnDefinition = "TEXT")
    private String writingStyle;

    @Column(name = "forbidden_words", columnDefinition = "TEXT")
    private String forbiddenWords; // JSON array string or comma separated

    @Column(name = "preferred_hashtags", columnDefinition = "TEXT")
    private String preferredHashtags; // JSON array string or space separated

    @Column(name = "default_cta", columnDefinition = "TEXT")
    private String defaultCta;

    @Builder.Default
    @Column(name = "emoji_policy", length = 30)
    private String emojiPolicy = "MODERATE"; // NONE, MINIMAL, MODERATE, EXPRESSIVE

    @Column(name = "extra_guidelines", columnDefinition = "TEXT")
    private String extraGuidelines;

    // AI Configuration
    @Builder.Default
    @Column(name = "text_provider_primary", length = 50)
    private String textProviderPrimary = "GEMINI";

    @Builder.Default
    @Column(name = "text_provider_fallback", length = 50)
    private String textProviderFallback = "OPENAI";

    @Builder.Default
    @Column(name = "image_provider_primary", length = 50)
    private String imageProviderPrimary = "GEMINI_IMAGE";

    @Builder.Default
    @Column(name = "image_provider_fallback", length = 50)
    private String imageProviderFallback = "OPENAI_IMAGE";

    @Builder.Default
    @Column(name = "ai_daily_budget_usd", precision = 8, scale = 2)
    private BigDecimal aiDailyBudgetUsd = new BigDecimal("5.00");

    @Builder.Default
    @Column(name = "append_ai_disclosure")
    private Boolean appendAiDisclosure = false;

    @Column(name = "ai_disclosure_text")
    private String aiDisclosureText;

    // Notifications
    @Column(name = "notify_telegram_chat_id", length = 100)
    private String notifyTelegramChatId;

    @Column(name = "notify_email")
    private String notifyEmail;

    @Column(name = "notify_events", columnDefinition = "TEXT")
    private String notifyEvents; // JSON array or comma separated event types

    @Builder.Default
    @Column(name = "daily_digest_enabled")
    private Boolean dailyDigestEnabled = true;

    @Builder.Default
    @Column(name = "daily_digest_time")
    private LocalTime dailyDigestTime = LocalTime.of(8, 0);

    // Operational Defaults
    @Builder.Default
    @Column(name = "generate_lead_hours")
    private Integer generateLeadHours = 12;

    @Builder.Default
    @Column(name = "missed_grace_minutes")
    private Integer missedGraceMinutes = 120;
}
