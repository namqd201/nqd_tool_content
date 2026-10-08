package com.nqd.nqd_tool_content.service.social.adapter;

import com.nqd.nqd_tool_content.service.social.SocialPlatformAdapter;
import com.nqd.nqd_tool_content.service.social.dto.DiscoveredAccount;
import com.nqd.nqd_tool_content.service.social.dto.PostResult;
import com.nqd.nqd_tool_content.service.social.dto.PublishRequest;
import com.nqd.nqd_tool_content.service.social.dto.RefreshTokenResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class FakeSocialAdapter implements SocialPlatformAdapter {

    @Override
    public String getPlatformCode() {
        return "FAKE";
    }

    @Override
    public List<DiscoveredAccount> discoverAccounts(String accessToken) {
        log.info("[FakeSocialAdapter] Discovering accounts with mock token");
        List<DiscoveredAccount> accounts = new ArrayList<>();
        accounts.add(DiscoveredAccount.builder()
                .platformAccountId("mock_acc_" + UUID.randomUUID().toString().substring(0, 8))
                .displayName("Demo Channel Page")
                .username("demochannel")
                .avatarUrl("https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=120&auto=format&fit=crop&q=80")
                .accountType("PAGE")
                .pageAccessToken("fake_page_token_" + UUID.randomUUID())
                .build());
        return accounts;
    }

    @Override
    public PostResult publish(PublishRequest request) {
        log.info("[FakeSocialAdapter] Publishing post for platform={}, dryRun={}", request.getPlatform(), request.isDryRun());
        String fakeId = "mock_post_" + UUID.randomUUID().toString().substring(0, 10);
        String fakeUrl = "https://" + request.getPlatform().toLowerCase() + ".com/p/" + fakeId;
        return PostResult.success(fakeId, fakeUrl, "{\"mock\": true, \"status\": \"success\"}");
    }

    @Override
    public boolean validateToken(String accessToken) {
        return accessToken != null && !accessToken.isBlank() && !accessToken.startsWith("invalid_");
    }

    @Override
    public RefreshTokenResult refreshToken(String refreshToken) {
        return RefreshTokenResult.builder()
                .successful(true)
                .newAccessToken("mock_new_access_token_" + UUID.randomUUID())
                .newRefreshToken("mock_new_refresh_token_" + UUID.randomUUID())
                .tokenExpiresAt(LocalDateTime.now().plusDays(60))
                .build();
    }
}
