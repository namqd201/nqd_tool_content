package com.nqd.nqd_tool_content.service.social.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublishRequest {
    private String platform;
    private String platformAccountId;
    private String accountType;
    private String accessToken; // Decrypted token
    private String pageAccessToken; // Decrypted page token nếu có
    private String text;
    private List<String> mediaUrls;
    private boolean dryRun;
}
