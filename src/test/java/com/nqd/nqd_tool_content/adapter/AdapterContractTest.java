package com.nqd.nqd_tool_content.adapter;

import com.nqd.nqd_tool_content.service.social.SocialPlatformAdapter;
import com.nqd.nqd_tool_content.service.social.adapter.*;
import com.nqd.nqd_tool_content.service.social.dto.DiscoveredAccount;
import com.nqd.nqd_tool_content.service.social.dto.PostResult;
import com.nqd.nqd_tool_content.service.social.dto.PublishRequest;
import com.nqd.nqd_tool_content.service.social.dto.RefreshTokenResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract Tests kiểm thử tuân thủ hợp đồng của toàn bộ các Social Platform Adapters (ADAPT-001 - ADAPT-005).
 */
public class AdapterContractTest {

    private final FacebookAdapter facebookAdapter = new FacebookAdapter();
    private final ThreadsAdapter threadsAdapter = new ThreadsAdapter();
    private final XAdapter xAdapter = new XAdapter();
    private final LinkedInAdapter linkedInAdapter = new LinkedInAdapter();
    private final FakeSocialAdapter fakeAdapter = new FakeSocialAdapter();

    private final List<SocialPlatformAdapter> allAdapters = List.of(
            facebookAdapter,
            threadsAdapter,
            xAdapter,
            linkedInAdapter,
            fakeAdapter
    );

    @Test
    @DisplayName("Mọi adapter phải có mã nền tảng và hỗ trợ token validation cơ bản")
    void testPlatformCodesAndTokenValidation() {
        for (SocialPlatformAdapter adapter : allAdapters) {
            assertNotNull(adapter.getPlatformCode());
            assertFalse(adapter.getPlatformCode().isBlank());

            assertTrue(adapter.validateToken("some_valid_token_string"));
            assertFalse(adapter.validateToken(null));
            assertFalse(adapter.validateToken("   "));
        }
    }

    @Test
    @DisplayName("Tất cả adapter phải hỗ trợ xuất bản ở chế độ Dry-Run an toàn")
    void testDryRunPublishingContract() {
        for (SocialPlatformAdapter adapter : allAdapters) {
            PublishRequest request = PublishRequest.builder()
                    .platform(adapter.getPlatformCode())
                    .platformAccountId("acc_123")
                    .accessToken("mock_token_123")
                    .text("Xin chào, đây là bài viết kiểm thử chế độ Dry-Run!")
                    .dryRun(true)
                    .build();

            PostResult result = adapter.publish(request);
            assertNotNull(result, "Adapter " + adapter.getPlatformCode() + " không được trả null");
            assertEquals("PUBLISHED", result.getStatus(), "Dry-run phải trả về trạng thái PUBLISHED");
            assertNotNull(result.getPlatformPostId());
            assertNotNull(result.getPostUrl());
        }
    }

    @Test
    @DisplayName("Adapter phải khám phá được ít nhất 1 account với mock/test token")
    void testAccountDiscoveryContract() {
        for (SocialPlatformAdapter adapter : allAdapters) {
            List<DiscoveredAccount> accounts = adapter.discoverAccounts("mock_token_test");
            assertNotNull(accounts);
            assertFalse(accounts.isEmpty(), "Adapter " + adapter.getPlatformCode() + " phải trả về danh sách kênh");

            DiscoveredAccount acc = accounts.get(0);
            assertNotNull(acc.getPlatformAccountId());
            assertNotNull(acc.getDisplayName());
        }
    }

    @Test
    @DisplayName("Facebook, Threads, X hỗ trợ tra cứu đối soát; LinkedIn không hỗ trợ")
    void testPublishedPostLookupSupport() {
        assertTrue(facebookAdapter.supportsPublishedPostLookup());
        assertTrue(threadsAdapter.supportsPublishedPostLookup());
        assertTrue(xAdapter.supportsPublishedPostLookup());
        assertFalse(linkedInAdapter.supportsPublishedPostLookup(), "LinkedIn quyền cá nhân không hỗ trợ tra cứu bài đăng");

        // Gọi thử findPublishedPost với mock
        PublishRequest req = PublishRequest.builder()
                .platform("FACEBOOK")
                .platformAccountId("page_123")
                .accessToken("mock_token")
                .text("Bài test")
                .build();

        PostResult fbLookup = facebookAdapter.findPublishedPost(req, Instant.now());
        assertNotNull(fbLookup);

        PostResult liLookup = linkedInAdapter.findPublishedPost(req, Instant.now());
        assertEquals("NEEDS_RECONCILE", liLookup.getStatus());
    }

    @Test
    @DisplayName("Adapter phải hỗ trợ refresh token và cấp hạn mới")
    void testRefreshTokenContract() {
        for (SocialPlatformAdapter adapter : allAdapters) {
            RefreshTokenResult result = adapter.refreshToken("mock_refresh_token");
            assertNotNull(result);
            assertTrue(result.isSuccessful());
            assertNotNull(result.getNewAccessToken());
            assertNotNull(result.getTokenExpiresAt());
        }
    }
}
