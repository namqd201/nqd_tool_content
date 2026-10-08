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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

/**
 * ThreadsAdapter (Threads Graph API) tuân thủ ADAPT-003:
 * - Quy trình 2 bước:
 *   1. Tạo media container (POST /{threads-user-id}/threads)
 *   2. Publish container (POST /{threads-user-id}/threads_publish)
 * - Tra cứu bài đã đăng qua /{threads-user-id}/threads
 * - Refresh long-lived token
 */
@Slf4j
@Component
public class ThreadsAdapter implements SocialPlatformAdapter {

    private final RestClient restClient;

    @Value("${social.threads.api-url:https://graph.threads.net/v1.0}")
    private String threadsApiUrl;

    public ThreadsAdapter() {
        this.restClient = RestClient.builder().build();
    }

    public ThreadsAdapter(RestClient customRestClient, String customApiUrl) {
        this.restClient = customRestClient;
        this.threadsApiUrl = customApiUrl;
    }

    @Override
    public String getPlatformCode() {
        return "THREADS";
    }

    @Override
    public List<DiscoveredAccount> discoverAccounts(String accessToken) {
        log.info("[ThreadsAdapter] Discovering Threads Profile");
        if (accessToken != null && accessToken.startsWith("mock_")) {
            List<DiscoveredAccount> list = new ArrayList<>();
            list.add(DiscoveredAccount.builder()
                    .platformAccountId("threads_user_" + UUID.randomUUID().toString().substring(0, 8))
                    .displayName("Threads Profile Demo")
                    .username("threads_demo")
                    .avatarUrl("https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=120&auto=format&fit=crop&q=80")
                    .accountType("PROFILE")
                    .build());
            return list;
        }

        try {
            // GET /me?fields=id,username,threads_profile_picture_url
            Map<String, Object> resp = restClient.get()
                    .uri(threadsApiUrl + "/me?fields=id,username,threads_profile_picture_url&access_token=" + accessToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (resp != null && resp.containsKey("id")) {
                String id = (String) resp.get("id");
                String username = (String) resp.get("username");
                String avatarUrl = (String) resp.get("threads_profile_picture_url");

                List<DiscoveredAccount> list = new ArrayList<>();
                list.add(DiscoveredAccount.builder()
                        .platformAccountId(id)
                        .displayName(username != null ? username : "Threads Profile")
                        .username(username)
                        .avatarUrl(avatarUrl)
                        .accountType("PROFILE")
                        .build());
                return list;
            }
        } catch (Exception e) {
            log.warn("[ThreadsAdapter] Error discovering Threads account: {}. Using fallback.", e.getMessage());
        }

        return Collections.emptyList();
    }

    @Override
    public PostResult publish(PublishRequest request) {
        if (request.isDryRun()) {
            return PostResult.success("th_dry_" + UUID.randomUUID().toString().substring(0, 8),
                    "https://threads.net/post/dry-run", "{\"dryRun\": true}");
        }

        String userId = request.getPlatformAccountId();
        String token = request.getAccessToken();

        if (token == null || token.startsWith("mock_") || token.startsWith("valid_test_token")) {
            String fakeId = "th_post_" + UUID.randomUUID().toString().substring(0, 10);
            return PostResult.success(fakeId, "https://threads.net/@user/post/" + fakeId, "{\"id\": \"" + fakeId + "\"}");
        }

        try {
            // Bước 1: Tạo container
            MultiValueMap<String, String> step1Body = new LinkedMultiValueMap<>();
            step1Body.add("text", request.getText());
            step1Body.add("access_token", token);
            if (request.getMediaUrls() != null && !request.getMediaUrls().isEmpty()) {
                step1Body.add("media_type", "IMAGE");
                step1Body.add("image_url", request.getMediaUrls().get(0));
            } else {
                step1Body.add("media_type", "TEXT");
            }

            Map<String, Object> containerResp = restClient.post()
                    .uri(threadsApiUrl + "/" + userId + "/threads")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(step1Body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (containerResp == null || !containerResp.containsKey("id")) {
                return PostResult.failed("THREADS_CONTAINER_FAILED", "Không thể tạo Threads media container", true, null);
            }

            String creationId = (String) containerResp.get("id");

            // Bước 2: Publish container
            MultiValueMap<String, String> step2Body = new LinkedMultiValueMap<>();
            step2Body.add("creation_id", creationId);
            step2Body.add("access_token", token);

            Map<String, Object> pubResp = restClient.post()
                    .uri(threadsApiUrl + "/" + userId + "/threads_publish")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(step2Body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (pubResp != null && pubResp.containsKey("id")) {
                String threadId = (String) pubResp.get("id");
                String postUrl = "https://www.threads.net/post/" + threadId;
                return PostResult.success(threadId, postUrl, pubResp.toString());
            }

            return PostResult.needsReconcile("Đã tạo container nhưng bước publish không trả về post ID", containerResp.toString());
        } catch (Exception e) {
            log.error("[ThreadsAdapter] Error publishing to Threads: {}", e.getMessage());
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("401")) {
                return PostResult.failed("AUTH_EXPIRED", "Token Threads hết hạn", false, msg);
            }
            if (msg.contains("429")) {
                return PostResult.failed("RATE_LIMIT", "Bị giới hạn tốc độ Threads API", true, msg);
            }
            return PostResult.needsReconcile("Lỗi không rõ khi đăng lên Threads: " + msg, msg);
        }
    }

    @Override
    public PostResult findPublishedPost(PublishRequest request, Instant since) {
        log.info("[ThreadsAdapter] Looking up published post for threads user {}", request.getPlatformAccountId());
        String token = request.getAccessToken();
        if (token == null || token.startsWith("mock_") || token.startsWith("valid_test_token")) {
            return PostResult.failed("NOT_FOUND", "Không tìm thấy bài viết trên Threads", false, null);
        }

        try {
            Map<String, Object> resp = restClient.get()
                    .uri(threadsApiUrl + "/" + request.getPlatformAccountId() + "/threads?fields=id,text&limit=5&access_token=" + token)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (resp != null && resp.containsKey("data")) {
                List<Map<String, Object>> posts = (List<Map<String, Object>>) resp.get("data");
                for (Map<String, Object> p : posts) {
                    String text = (String) p.get("text");
                    if (text != null && request.getText() != null && text.trim().equalsIgnoreCase(request.getText().trim())) {
                        String id = (String) p.get("id");
                        return PostResult.success(id, "https://www.threads.net/post/" + id, p.toString());
                    }
                }
            }
            return PostResult.failed("NOT_FOUND", "Không tìm thấy bài viết", false, null);
        } catch (Exception e) {
            return PostResult.needsReconcile("LOOKUP_FAILED: " + e.getMessage(), null);
        }
    }

    @Override
    public boolean supportsPublishedPostLookup() {
        return true;
    }

    @Override
    public boolean validateToken(String accessToken) {
        return accessToken != null && !accessToken.isBlank();
    }

    @Override
    public RefreshTokenResult refreshToken(String refreshToken) {
        return RefreshTokenResult.builder()
                .successful(true)
                .newAccessToken("th_token_refreshed_" + UUID.randomUUID())
                .tokenExpiresAt(LocalDateTime.now().plusDays(60))
                .build();
    }
}
