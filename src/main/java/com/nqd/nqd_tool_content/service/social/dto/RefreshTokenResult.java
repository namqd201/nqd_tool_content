package com.nqd.nqd_tool_content.service.social.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenResult {
    private boolean successful;
    private String newAccessToken;
    private String newRefreshToken;
    private LocalDateTime tokenExpiresAt;
    private LocalDateTime refreshTokenExpiresAt;
    private String errorMessage;
}
