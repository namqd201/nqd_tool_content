package com.nqd.nqd_tool_content.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialConnectionResponse {
    private String id;
    private String platform;
    private String platformUserId;
    private String displayName;
    private String status;
    private String scopes;
    private LocalDateTime tokenExpiresAt;
    private LocalDateTime refreshTokenExpiresAt;
    private LocalDateTime lastRefreshedAt;
    private Integer refreshFailureCount;
    private String lastError;
    private LocalDateTime connectedAt;
    @Builder.Default
    private List<SocialAccountResponse> accounts = new ArrayList<>();
}
