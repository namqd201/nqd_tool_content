package com.nqd.nqd_tool_content.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "social_connections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SocialConnection extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "platform", nullable = false, length = 50)
    private String platform; // FACEBOOK, THREADS, X, LINKEDIN

    @Column(name = "platform_user_id", nullable = false, length = 255)
    private String platformUserId;

    @Column(name = "display_name", length = 255)
    private String displayName;

    @Column(name = "scopes", columnDefinition = "TEXT")
    private String scopes;

    @Builder.Default
    @Column(name = "status", nullable = false, length = 30)
    private String status = "ACTIVE"; // ACTIVE, EXPIRED, REVOKED, ERROR

    @Column(name = "access_token_enc", columnDefinition = "TEXT", nullable = false)
    private String accessTokenEnc;

    @Column(name = "refresh_token_enc", columnDefinition = "TEXT")
    private String refreshTokenEnc;

    @Builder.Default
    @Column(name = "key_version", nullable = false)
    private Integer keyVersion = 1;

    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;

    @Column(name = "refresh_token_expires_at")
    private LocalDateTime refreshTokenExpiresAt;

    @Column(name = "last_refreshed_at")
    private LocalDateTime lastRefreshedAt;

    @Builder.Default
    @Column(name = "refresh_failure_count")
    private Integer refreshFailureCount = 0;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "connected_at")
    private LocalDateTime connectedAt;

    @PrePersist
    @Override
    protected void onCreate() {
        super.onCreate();
        if (connectedAt == null) {
            connectedAt = LocalDateTime.now();
        }
    }
}
