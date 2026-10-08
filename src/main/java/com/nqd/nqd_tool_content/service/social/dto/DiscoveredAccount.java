package com.nqd.nqd_tool_content.service.social.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscoveredAccount {
    private String platformAccountId;
    private String displayName;
    private String username;
    private String avatarUrl;
    private String accountType; // PAGE, PROFILE, ORGANIZATION
    private String pageAccessToken; // Cho Facebook Page
}
