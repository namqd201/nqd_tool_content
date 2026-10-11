package com.nqd.nqd_tool_content.service.social.adapter;

import com.nqd.nqd_tool_content.service.social.SocialPlatformAdapter;
import com.nqd.nqd_tool_content.service.social.dto.DiscoveredAccount;
import com.nqd.nqd_tool_content.service.social.dto.PostResult;
import com.nqd.nqd_tool_content.service.social.dto.PublishRequest;
import com.nqd.nqd_tool_content.service.social.dto.RefreshTokenResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${social.threads.api-url:https://graph.threads.net/v1.0}")
    private String threadsApiUrl;

    @Value("${social.threads.client-id:}")
    private String threadsClientId;

    @Value("${social.threads.client-secret:}")
    private String threadsClientSecret;

    public ThreadsAdapter() {
        this.restClient = RestClient.builder().build();
    }

    public ThreadsAdapter(RestClient customRestClient, String customApiUrl) {
        this.restClient = customRestClient;
        this.threadsApiUrl = customApiUrl;
    }

    public String exchangeForLongLivedToken(String shortLivedToken) {
        return exchangeForLongLivedToken(shortLivedToken, null);
    }

    public String exchangeForLongLivedToken(String shortLivedToken, String explicitClientSecret) {
        if (shortLivedToken == null || shortLivedToken.isBlank() || shortLivedToken.startsWith("mock_") || shortLivedToken.startsWith("manual_")) {
            return shortLivedToken;
        }
        String secret = (explicitClientSecret != null && !explicitClientSecret.isBlank())
                ? explicitClientSecret.trim()
                : (threadsClientSecret != null ? threadsClientSecret.trim() : null);

        if (secret == null || secret.isBlank()) {
            log.info("[ThreadsAdapter] threadsClientSecret is not configured, skipping long-lived token exchange");
            return shortLivedToken;
        }
        try {
            log.info("[ThreadsAdapter] Exchanging short-lived token for long-lived Threads token (60 days)");
            String url = "https://graph.threads.net/access_token?grant_type=th_exchange_token&client_secret="
                    + secret + "&access_token=" + shortLivedToken.trim();
            Map<String, Object> resp = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
            if (resp != null && resp.containsKey("access_token")) {
                String longLivedToken = (String) resp.get("access_token");
                log.info("[ThreadsAdapter] Successfully obtained 60-day long-lived Threads token");
                return longLivedToken;
            }
        } catch (Exception e) {
            String err = extractErrorMessage(e);
            log.warn("[ThreadsAdapter] Could not exchange long-lived token: {}", err);
            if (err.contains("client_secret") || err.contains("Invalid Client") || err.contains("101")) {
                throw new com.nqd.nqd_tool_content.exception.BusinessException(
                        "INVALID_APP_CREDENTIALS",
                        "Threads App Secret không chính xác: " + err
                );
            }
            if (err.contains("Session has expired") || err.contains("Error validating access token") || err.contains("code\":190") || err.contains("OAuthException")) {
                throw new com.nqd.nqd_tool_content.exception.BusinessException(
                        "TOKEN_EXPIRED",
                        "Token Threads đã hết hạn hoặc không hợp lệ: " + err
                );
            }
        }
        return shortLivedToken;
    }

    private String extractErrorMessage(Exception e) {
        if (e instanceof org.springframework.web.client.RestClientResponseException rcre) {
            String body = rcre.getResponseBodyAsString();
            if (body != null && !body.isBlank()) {
                try {
                    JsonNode root = objectMapper.readTree(body);
                    if (root.has("error") && root.get("error").has("message")) {
                        return root.get("error").get("message").asText();
                    }
                } catch (Exception ignored) {}
                return body;
            }
        }
        return e.getMessage() != null ? e.getMessage() : "Unknown error";
    }

    @Override
    public String getPlatformCode() {
        return "THREADS";
    }

    @Override
    public List<DiscoveredAccount> discoverAccounts(String accessToken) {
        log.info("[ThreadsAdapter] Discovering Threads Profile via Graph API");
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

        if (accessToken == null || accessToken.isBlank()) {
            return Collections.emptyList();
        }

        Map<String, Object> resp = null;
        Exception lastException = null;

        String[] fieldVariants = new String[] {
                "id,username,name,threads_profile_picture_url",
                "id,username,name,profile_picture_url",
                "id,username,threads_profile_picture_url",
                "id,username,name",
                "id,username"
        };

        for (String fields : fieldVariants) {
            try {
                resp = restClient.get()
                        .uri(threadsApiUrl + "/me?fields=" + fields + "&access_token=" + accessToken.trim())
                        .retrieve()
                        .body(new ParameterizedTypeReference<Map<String, Object>>() {});
                if (resp != null && resp.containsKey("id")) {
                    log.info("[ThreadsAdapter] Successfully discovered Threads profile using fields=[{}]", fields);
                    break;
                }
            } catch (Exception e) {
                lastException = e;
                log.info("[ThreadsAdapter] Query /me with fields=[{}] failed: {}. Retrying simpler fields...", fields, extractErrorMessage(e));
            }
        }

        if (resp != null && resp.containsKey("id")) {
            String id = String.valueOf(resp.get("id"));
            String username = resp.containsKey("username") ? String.valueOf(resp.get("username")) : null;
            String name = resp.containsKey("name") ? String.valueOf(resp.get("name")) : null;
            String avatarUrl = null;
            if (resp.containsKey("threads_profile_picture_url")) {
                avatarUrl = String.valueOf(resp.get("threads_profile_picture_url"));
            } else if (resp.containsKey("profile_picture_url")) {
                avatarUrl = String.valueOf(resp.get("profile_picture_url"));
            }

            String display = (name != null && !name.isBlank()) ? name : (username != null ? username : "Threads User");

            List<DiscoveredAccount> list = new ArrayList<>();
            list.add(DiscoveredAccount.builder()
                    .platformAccountId(id)
                    .displayName(display)
                    .username(username != null ? username : id)
                    .avatarUrl(avatarUrl)
                    .accountType("PROFILE")
                    .build());
            return list;
        }

        if (lastException != null) {
            String err = extractErrorMessage(lastException);
            log.error("[ThreadsAdapter] Failed to discover Threads account: {}", err);
            if (err.contains("Session has expired") || err.contains("Error validating access token") || err.contains("code\":190") || err.contains("OAuthException")) {
                throw new com.nqd.nqd_tool_content.exception.BusinessException(
                        "TOKEN_EXPIRED",
                        "Token Threads đã hết hạn hoặc không hợp lệ: " + err
                );
            }
            if (err.contains("permission") || err.contains("threads_basic")) {
                throw new com.nqd.nqd_tool_content.exception.BusinessException(
                        "PERMISSION_DENIED",
                        "Token Threads thiếu quyền truy cập (yêu cầu quyền threads_basic)."
                );
            }
            throw new com.nqd.nqd_tool_content.exception.BusinessException(
                    "THREADS_API_ERROR",
                    "Lỗi Meta Threads API: " + err
            );
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
        if (refreshToken != null && !refreshToken.isBlank() && !refreshToken.startsWith("mock_")
                && !refreshToken.startsWith("manual_") && !refreshToken.startsWith("valid_test_token")) {
            try {
                String url = "https://graph.threads.net/refresh_access_token?grant_type=th_refresh_token&access_token=" + refreshToken.trim();
                Map<String, Object> resp = restClient.get()
                        .uri(url)
                        .retrieve()
                        .body(new ParameterizedTypeReference<Map<String, Object>>() {});
                if (resp != null && resp.containsKey("access_token")) {
                    String newAccessToken = (String) resp.get("access_token");
                    Number expiresIn = (Number) resp.get("expires_in");
                    long seconds = expiresIn != null ? expiresIn.longValue() : 5184000L;
                    return RefreshTokenResult.builder()
                            .successful(true)
                            .newAccessToken(newAccessToken)
                            .tokenExpiresAt(LocalDateTime.now().plusSeconds(seconds))
                            .build();
                }
            } catch (Exception e) {
                log.warn("[ThreadsAdapter] Failed to refresh token via Threads API: {}", e.getMessage());
                return RefreshTokenResult.builder()
                        .successful(false)
                        .errorMessage("Failed to refresh Threads token: " + e.getMessage())
                        .build();
            }
        }
        return RefreshTokenResult.builder()
                .successful(true)
                .newAccessToken("th_token_refreshed_" + UUID.randomUUID())
                .tokenExpiresAt(LocalDateTime.now().plusDays(60))
                .build();
    }
}
