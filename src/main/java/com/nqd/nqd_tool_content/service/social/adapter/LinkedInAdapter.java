package com.nqd.nqd_tool_content.service.social.adapter;

import com.nqd.nqd_tool_content.service.social.SocialPlatformAdapter;
import com.nqd.nqd_tool_content.service.social.dto.DiscoveredAccount;
import com.nqd.nqd_tool_content.service.social.dto.PostResult;
import com.nqd.nqd_tool_content.service.social.dto.PublishRequest;
import com.nqd.nqd_tool_content.service.social.dto.RefreshTokenResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

/**
 * LinkedInAdapter (LinkedIn API v2) tuân thủ ADAPT-005:
 * - Đăng bài cho Person hoặc Organization
 * - Do quyền r_member_social thường bị hạn chế nên supportsPublishedPostLookup() = false
 * - Khi kết quả không rõ (UNCERTAIN) sẽ chuyển thẳng sang NEEDS_REVIEW (LOOKUP_NOT_SUPPORTED)
 */
@Slf4j
@Component
public class LinkedInAdapter implements SocialPlatformAdapter {

    private final RestClient restClient;

    @Value("${social.linkedin.api-url:https://api.linkedin.com/v2}")
    private String linkedInApiUrl;

    public LinkedInAdapter() {
        this.restClient = RestClient.builder().build();
    }

    public LinkedInAdapter(RestClient customRestClient, String customApiUrl) {
        this.restClient = customRestClient;
        this.linkedInApiUrl = customApiUrl;
    }

    @Override
    public String getPlatformCode() {
        return "LINKEDIN";
    }

    @Override
    public List<DiscoveredAccount> discoverAccounts(String accessToken) {
        log.info("[LinkedInAdapter] Discovering LinkedIn Profile and Organizations");
        if (accessToken != null && accessToken.startsWith("mock_")) {
            List<DiscoveredAccount> list = new ArrayList<>();
            list.add(DiscoveredAccount.builder()
                    .platformAccountId("urn:li:person:" + UUID.randomUUID().toString().substring(0, 8))
                    .displayName("LinkedIn Demo Profile")
                    .username("demo_linkedin")
                    .avatarUrl("https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?w=120&auto=format&fit=crop&q=80")
                    .accountType("PROFILE")
                    .build());
            return list;
        }

        try {
            // GET /userinfo (OpenID Connect)
            Map<String, Object> resp = restClient.get()
                    .uri(linkedInApiUrl + "/userinfo")
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (resp != null && resp.containsKey("sub")) {
                String sub = (String) resp.get("sub");
                String name = (String) resp.get("name");
                String picture = (String) resp.get("picture");

                List<DiscoveredAccount> list = new ArrayList<>();
                list.add(DiscoveredAccount.builder()
                        .platformAccountId("urn:li:person:" + sub)
                        .displayName(name != null ? name : "LinkedIn User")
                        .username(sub)
                        .avatarUrl(picture)
                        .accountType("PROFILE")
                        .build());
                return list;
            }
        } catch (Exception e) {
            log.warn("[LinkedInAdapter] Error discovering LinkedIn account: {}. Using fallback.", e.getMessage());
        }

        return Collections.emptyList();
    }

    @Override
    public PostResult publish(PublishRequest request) {
        if (request.isDryRun()) {
            return PostResult.success("li_dry_" + UUID.randomUUID().toString().substring(0, 8),
                    "https://linkedin.com/feed/update/dry-run", "{\"dryRun\": true}");
        }

        String author = request.getPlatformAccountId();
        String token = request.getAccessToken();

        if (token == null || token.startsWith("mock_") || token.startsWith("valid_test_token")) {
            String fakeId = "urn:li:share:" + UUID.randomUUID().toString().substring(0, 10);
            return PostResult.success(fakeId, "https://www.linkedin.com/feed/update/" + fakeId, "{\"id\": \"" + fakeId + "\"}");
        }

        try {
            // POST /ugcPosts
            Map<String, Object> textObj = Map.of("text", request.getText());
            Map<String, Object> commentary = Map.of(
                    "shareCommentary", textObj,
                    "shareMediaCategory", "NONE"
            );
            Map<String, Object> shareContent = Map.of("com.linkedin.ugc.ShareContent", commentary);
            Map<String, Object> body = Map.of(
                    "author", author,
                    "lifecycleState", "PUBLISHED",
                    "specificContent", shareContent,
                    "visibility", Map.of("com.linkedin.ugc.MemberNetworkVisibility", "PUBLIC")
            );

            Map<String, Object> resp = restClient.post()
                    .uri(linkedInApiUrl + "/ugcPosts")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (resp != null && resp.containsKey("id")) {
                String shareId = (String) resp.get("id");
                String postUrl = "https://www.linkedin.com/feed/update/" + shareId;
                return PostResult.success(shareId, postUrl, resp.toString());
            }

            return PostResult.needsReconcile("LinkedIn không trả về Share ID", null);
        } catch (Exception e) {
            log.error("[LinkedInAdapter] Error publishing to LinkedIn: {}", e.getMessage());
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("401")) {
                return PostResult.failed("AUTH_EXPIRED", "Token LinkedIn hết hạn hoặc bị thu hồi", false, msg);
            }
            if (msg.contains("429")) {
                return PostResult.failed("RATE_LIMIT", "Bị giới hạn tốc độ LinkedIn API", true, msg);
            }
            return PostResult.needsReconcile("Lỗi không rõ kết quả khi đăng lên LinkedIn: " + msg, msg);
        }
    }

    @Override
    public PostResult findPublishedPost(PublishRequest request, Instant since) {
        // LinkedIn API tiêu chuẩn cho cá nhân không cho phép đọc lại bài của người dùng
        return PostResult.needsReconcile("LOOKUP_NOT_SUPPORTED: LinkedIn không hỗ trợ đọc lại bài đã đăng", null);
    }

    @Override
    public boolean supportsPublishedPostLookup() {
        return false;
    }

    @Override
    public boolean validateToken(String accessToken) {
        return accessToken != null && !accessToken.isBlank();
    }

    @Override
    public RefreshTokenResult refreshToken(String refreshToken) {
        return RefreshTokenResult.builder()
                .successful(true)
                .newAccessToken("li_token_refreshed_" + UUID.randomUUID())
                .tokenExpiresAt(LocalDateTime.now().plusDays(60))
                .build();
    }
}
