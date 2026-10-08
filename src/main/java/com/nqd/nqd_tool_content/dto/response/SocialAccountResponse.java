package com.nqd.nqd_tool_content.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialAccountResponse {
    private String id;
    private String connectionId;
    private String platform;
    private String accountType;
    private String platformAccountId;
    private String displayName;
    private String username;
    private String avatarUrl;
    private Boolean isEnabled;
    private String status;
    private LocalDateTime lastHealthCheckAt;
    private String lastError;
    private LocalDateTime needsReauthAt;
}
