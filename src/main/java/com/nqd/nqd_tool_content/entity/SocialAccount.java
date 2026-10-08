package com.nqd.nqd_tool_content.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "social_accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SocialAccount extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "connection_id", nullable = false)
    private UUID connectionId;

    @Column(name = "platform", nullable = false, length = 50)
    private String platform; // FACEBOOK, THREADS, X, LINKEDIN

    @Builder.Default
    @Column(name = "account_type", nullable = false, length = 50)
    private String accountType = "PAGE"; // PAGE, PROFILE, ORGANIZATION

    @Column(name = "platform_account_id", nullable = false, length = 255)
    private String platformAccountId;

    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;

    @Column(name = "username", length = 255)
    private String username;

    @Column(name = "avatar_url", length = 1000)
    private String avatarUrl;

    @Builder.Default
    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled = false;

    @Builder.Default
    @Column(name = "status", nullable = false, length = 30)
    private String status = "ACTIVE"; // ACTIVE, EXPIRED, REVOKED, ERROR

    @Column(name = "page_access_token_enc", columnDefinition = "TEXT")
    private String pageAccessTokenEnc;

    @Builder.Default
    @Column(name = "key_version")
    private Integer keyVersion = 1;

    @Column(name = "last_health_check_at")
    private LocalDateTime lastHealthCheckAt;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "needs_reauth_at")
    private LocalDateTime needsReauthAt;
}
