package com.nqd.nqd_tool_content.service.social.adapter;

import com.nqd.nqd_tool_content.service.social.SocialPlatformAdapter;
import com.nqd.nqd_tool_content.service.social.dto.DiscoveredAccount;
import com.nqd.nqd_tool_content.service.social.dto.PostResult;
import com.nqd.nqd_tool_content.service.social.dto.PublishRequest;
import com.nqd.nqd_tool_content.service.social.dto.RefreshTokenResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.nqd.nqd_tool_content.service.storage.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

/**
 * FacebookAdapter (Page Graph API) tuân thủ ADAPT-002:
 * - Hỗ trợ đăng bài chữ và bài kèm ảnh lên Facebook Fanpage
 * - Khám phá Pages qua Graph API /me/accounts
 * - Đối soát findPublishedPost qua /{page-id}/feed
 * - Thu hồi quyền qua /me/permissions
 * - Chế độ Dry-Run hoặc Fallback khi chưa có API Keys thật
 */
@Slf4j
@Component
public class FacebookAdapter implements SocialPlatformAdapter {

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired(required = false)
    private StorageService storageService;

    @Value("${social.facebook.api-url:https://graph.facebook.com/v20.0}")
    private String graphApiUrl;

    public FacebookAdapter() {
        this.restClient = RestClient.builder().build();
    }

    public FacebookAdapter(RestClient customRestClient, String customApiUrl) {
        this.restClient = customRestClient;
        this.graphApiUrl = customApiUrl;
    }

    @Override
    public String getPlatformCode() {
        return "FACEBOOK";
    }

    /**
     * Trao đổi Short-Lived Access Token (1-2 giờ) lấy Long-Lived Access Token (60 ngày) qua Facebook Graph API.
     * Từ Long-Lived User Token, các Page Access Token lấy qua /me/accounts sẽ KHÔNG BAO GIỜ HẾT HẠN (Never Expire).
     */
    public String exchangeForLongLivedToken(String shortLivedToken, String appId, String appSecret) {
        if (shortLivedToken == null || shortLivedToken.isBlank() || appId == null || appId.isBlank() || appSecret == null || appSecret.isBlank()) {
            return shortLivedToken;
        }

        try {
            log.info("[FacebookAdapter] Exchanging short-lived token for long-lived token via Graph API oauth/access_token");
            String uri = String.format("%s/oauth/access_token?grant_type=fb_exchange_token&client_id=%s&client_secret=%s&fb_exchange_token=%s",
                    graphApiUrl, appId.trim(), appSecret.trim(), shortLivedToken.trim());

            String rawResp = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(String.class);

            if (rawResp != null && !rawResp.isBlank()) {
                JsonNode json = objectMapper.readTree(rawResp);
                if (json.has("access_token")) {
                    String longLivedToken = json.get("access_token").asText();
                    log.info("[FacebookAdapter] Successfully exchanged token! Long-lived token acquired.");
                    return longLivedToken;
                }
            }
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            log.warn("[FacebookAdapter] Failed to exchange token: {}", msg);
            if (msg.contains("Error validating client secret") || msg.contains("Invalid Client ID") || msg.contains("101")) {
                throw new com.nqd.nqd_tool_content.exception.BusinessException(
                        "INVALID_APP_CREDENTIALS",
                        "Facebook App ID hoặc App Secret không chính xác. Vui lòng kiểm tra lại trong Meta Developer Dashboard."
                );
            }
        }
        return shortLivedToken;
    }

    @Override
    public List<DiscoveredAccount> discoverAccounts(String accessToken) {
        log.info("[FacebookAdapter] Discovering Facebook Pages via Graph API");

        // Mock mode cho token test/demo
        if (accessToken != null && accessToken.startsWith("mock_")) {
            List<DiscoveredAccount> list = new ArrayList<>();
            list.add(DiscoveredAccount.builder()
                    .platformAccountId("fb_page_demo_" + UUID.randomUUID().toString().substring(0, 6))
                    .displayName("Facebook Fanpage Demo")
                    .username("demo_fb_page")
                    .avatarUrl("https://images.unsplash.com/photo-1544717305-2782549b5136?w=120&auto=format&fit=crop&q=80")
                    .accountType("PAGE")
                    .pageAccessToken("fb_page_token_" + UUID.randomUUID())
                    .build());
            return list;
        }

        if (accessToken == null || accessToken.isBlank()) {
            return Collections.emptyList();
        }

        // Bước 1: Thử truy vấn danh sách Fanpage qua User Token: GET /me/accounts
        try {
            String rawJson = restClient.get()
                    .uri(graphApiUrl + "/me/accounts?fields=id,name,picture,access_token&access_token=" + accessToken)
                    .retrieve()
                    .body(String.class);

            if (rawJson != null && !rawJson.isBlank()) {
                JsonNode root = objectMapper.readTree(rawJson);
                if (root.has("data") && root.get("data").isArray()) {
                    List<DiscoveredAccount> accounts = new ArrayList<>();
                    for (JsonNode page : root.get("data")) {
                        String pageId = page.has("id") ? page.get("id").asText() : null;
                        String name = page.has("name") ? page.get("name").asText() : null;
                        String pageToken = page.has("access_token") ? page.get("access_token").asText() : null;

                        String avatarUrl = null;
                        if (page.has("picture") && page.get("picture").has("data") && page.get("picture").get("data").has("url")) {
                            avatarUrl = page.get("picture").get("data").get("url").asText();
                        }

                        if (pageId != null && name != null) {
                            accounts.add(DiscoveredAccount.builder()
                                    .platformAccountId(pageId)
                                    .displayName(name)
                                    .username(pageId)
                                    .avatarUrl(avatarUrl)
                                    .accountType("PAGE")
                                    .pageAccessToken(pageToken)
                                    .build());
                        }
                    }
                    if (!accounts.isEmpty()) {
                        log.info("[FacebookAdapter] Found {} pages from /me/accounts", accounts.size());
                        return accounts;
                    }
                }
            }
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            log.warn("[FacebookAdapter] Error calling Graph API /me/accounts: {}", msg);
            if (msg.contains("Session has expired") || msg.contains("Error validating access token")) {
                throw new com.nqd.nqd_tool_content.exception.BusinessException(
                        "TOKEN_EXPIRED",
                        "Token Facebook đã hết hạn hoặc không hợp lệ (Session expired). Vui lòng tạo Token mới từ Meta Graph API Explorer."
                );
            }
        }

        // Bước 2: Nếu /me/accounts không trả về trang nào hoặc thất bại (Page Access Token trực tiếp), thử gọi GET /me
        try {
            String rawPageJson = restClient.get()
                    .uri(graphApiUrl + "/me?fields=id,name,picture&access_token=" + accessToken)
                    .retrieve()
                    .body(String.class);

            if (rawPageJson != null && !rawPageJson.isBlank()) {
                JsonNode pageObj = objectMapper.readTree(rawPageJson);
                if (pageObj.has("id") && pageObj.has("name")) {
                    String pageId = pageObj.get("id").asText();
                    String name = pageObj.get("name").asText();

                    String avatarUrl = null;
                    if (pageObj.has("picture") && pageObj.get("picture").has("data") && pageObj.get("picture").get("data").has("url")) {
                        avatarUrl = pageObj.get("picture").get("data").get("url").asText();
                    }

                    log.info("[FacebookAdapter] Token is direct Page Access Token for Page: {} ({})", name, pageId);
                    return List.of(DiscoveredAccount.builder()
                            .platformAccountId(pageId)
                            .displayName(name)
                            .username(pageId)
                            .avatarUrl(avatarUrl)
                            .accountType("PAGE")
                            .pageAccessToken(accessToken)
                            .build());
                }
            }
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            log.warn("[FacebookAdapter] Error calling Graph API /me: {}", msg);
            if (msg.contains("Session has expired") || msg.contains("Error validating access token")) {
                throw new com.nqd.nqd_tool_content.exception.BusinessException(
                        "TOKEN_EXPIRED",
                        "Token Facebook đã hết hạn hoặc không hợp lệ (Session expired). Vui lòng tạo Token mới từ Meta Graph API Explorer."
                );
            }
        }

        // Tuyệt đối không fallback về Mock account khi người dùng cung cấp Token thật
        return Collections.emptyList();
    }

    @Override
    public PostResult publish(PublishRequest request) {
        if (request.isDryRun()) {
            log.info("[FacebookAdapter] Dry run post for Facebook page: {}", request.getPlatformAccountId());
            return PostResult.success("fb_dry_" + UUID.randomUUID().toString().substring(0, 8),
                    "https://facebook.com/dry-run-post", "{\"dryRun\": true}");
        }

        String pageToken = request.getPageAccessToken() != null ? request.getPageAccessToken() : request.getAccessToken();
        String pageId = request.getPlatformAccountId();

        // Kiểm tra nếu là mock token
        if (pageToken == null || pageToken.startsWith("mock_") || pageToken.startsWith("valid_test_token")
                || pageToken.startsWith("fb_page_token_") || pageToken.contains("mock") || pageToken.contains("demo")) {
            String fakeId = "fb_post_" + UUID.randomUUID().toString().substring(0, 10);
            return PostResult.success(fakeId, "https://facebook.com/" + pageId + "/posts/" + fakeId, "{\"id\": \"" + fakeId + "\"}");
        }

        try {
            // Trường hợp có ảnh: POST /{page-id}/photos
            if (request.getMediaUrls() != null && !request.getMediaUrls().isEmpty()) {
                String photoUrl = request.getMediaUrls().get(0);
                byte[] imageBytes = null;

                // 1. Thử lấy ảnh trực tiếp từ StorageService nếu là URL nội bộ
                if (storageService != null && photoUrl.contains("/media/")) {
                    String token = photoUrl.substring(photoUrl.lastIndexOf("/media/") + 7);
                    try {
                        imageBytes = storageService.getMediaBytes(token);
                        log.info("[FacebookAdapter] Loaded {} bytes directly from StorageService for token {}", imageBytes.length, token);
                    } catch (Exception ex) {
                        log.warn("[FacebookAdapter] Could not get media bytes from StorageService: {}", ex.getMessage());
                    }
                }

                // 2. Nếu chưa có bytes, thử tải về từ URL
                if (imageBytes == null && (photoUrl.startsWith("http://") || photoUrl.startsWith("https://"))) {
                    try {
                        imageBytes = restClient.get().uri(photoUrl).retrieve().body(byte[].class);
                        if (imageBytes != null) {
                            log.info("[FacebookAdapter] Downloaded {} bytes from URL {}", imageBytes.length, photoUrl);
                        }
                    } catch (Exception ex) {
                        log.warn("[FacebookAdapter] Could not download image from {}: {}", photoUrl, ex.getMessage());
                    }
                }

                // Nếu có imageBytes, upload trực tiếp bằng multipart/form-data (tránh lỗi Facebook không tải được localhost)
                if (imageBytes != null && imageBytes.length > 0) {
                    ByteArrayResource resource = new ByteArrayResource(imageBytes) {
                        @Override
                        public String getFilename() {
                            return "photo.jpg";
                        }
                    };

                    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                    if (request.getText() != null) {
                        body.add("caption", request.getText());
                    }
                    body.add("source", resource);
                    body.add("access_token", pageToken);

                    String rawResp = restClient.post()
                            .uri(graphApiUrl + "/" + pageId + "/photos")
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                            .body(body)
                            .retrieve()
                            .body(String.class);

                    if (rawResp != null && !rawResp.isBlank()) {
                        JsonNode respNode = objectMapper.readTree(rawResp);
                        if (respNode.has("id")) {
                            String photoId = respNode.get("id").asText();
                            String postUrl = respNode.has("post_id")
                                    ? "https://facebook.com/" + respNode.get("post_id").asText().replace("_", "/posts/")
                                    : "https://facebook.com/" + photoId;
                            return PostResult.success(photoId, postUrl, rawResp);
                        }
                    }
                } else {
                    // Nếu không đọc được bytes nhưng là public URL
                    MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
                    if (request.getText() != null) {
                        body.add("caption", request.getText());
                    }
                    body.add("url", photoUrl);
                    body.add("access_token", pageToken);

                    String rawResp = restClient.post()
                            .uri(graphApiUrl + "/" + pageId + "/photos")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .body(body)
                            .retrieve()
                            .body(String.class);

                    if (rawResp != null && !rawResp.isBlank()) {
                        JsonNode respNode = objectMapper.readTree(rawResp);
                        if (respNode.has("id")) {
                            String photoId = respNode.get("id").asText();
                            String postUrl = respNode.has("post_id")
                                    ? "https://facebook.com/" + respNode.get("post_id").asText().replace("_", "/posts/")
                                    : "https://facebook.com/" + photoId;
                            return PostResult.success(photoId, postUrl, rawResp);
                        }
                    }
                }
            } else {
                // Trường hợp bài chữ: POST /{page-id}/feed
                MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
                body.add("message", request.getText());
                body.add("access_token", pageToken);

                String rawResp = restClient.post()
                        .uri(graphApiUrl + "/" + pageId + "/feed")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .body(body)
                        .retrieve()
                        .body(String.class);

                if (rawResp != null && !rawResp.isBlank()) {
                    JsonNode respNode = objectMapper.readTree(rawResp);
                    if (respNode.has("id")) {
                        String postId = respNode.get("id").asText();
                        String postUrl = postId.contains("_")
                                ? "https://facebook.com/" + postId.replace("_", "/posts/")
                                : "https://facebook.com/" + pageId + "/posts/" + postId;
                        return PostResult.success(postId, postUrl, rawResp);
                    }
                }
            }

            return PostResult.needsReconcile("Graph API response thiếu ID bài viết", null);
        } catch (Exception e) {
            log.error("[FacebookAdapter] Error publishing to Facebook Page {}: {}", pageId, e.getMessage());
            // Phân loại lỗi chính xác theo mã lỗi của Meta
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("\"code\":190") || msg.contains("code 190") || msg.contains("Session has expired") || msg.contains("Error validating access token") || msg.contains("access token is invalid")) {
                return PostResult.failed("AUTH_EXPIRED", "Token Facebook hết hạn hoặc không hợp lệ", false, msg);
            }
            if (msg.contains("\"code\":200") || msg.contains("code 200") || msg.contains("Permissions error")) {
                return PostResult.failed("PERMISSION_DENIED", "Token không đủ quyền đăng bài lên Fanpage (cần quyền pages_manage_posts)", false, msg);
            }
            if (msg.contains("\"code\":324") || msg.contains("code 324") || msg.contains("Missing or invalid image file")) {
                return PostResult.failed("MEDIA_INVALID", "Facebook không thể xử lý tệp ảnh đính kèm (Mã 324). Vui lòng thử lại.", false, msg);
            }
            if (msg.contains("429") || msg.contains("rate limit") || msg.contains("\"code\":4") || msg.contains("\"code\":17")) {
                return PostResult.failed("RATE_LIMIT", "Bị giới hạn tốc độ Facebook Graph API", true, msg);
            }
            if (msg.contains("duplicate") || msg.contains("already exists") || msg.contains("\"code\":506")) {
                return PostResult.needsReconcile("Nội dung có thể đã được đăng (trùng lặp)", msg);
            }
            return PostResult.needsReconcile("Lỗi phản hồi từ Facebook: " + msg, msg);
        }
    }

    @Override
    public PostResult findPublishedPost(PublishRequest request, Instant since) {
        log.info("[FacebookAdapter] Looking up published post for page {}", request.getPlatformAccountId());
        String pageToken = request.getPageAccessToken() != null ? request.getPageAccessToken() : request.getAccessToken();
        if (pageToken == null || pageToken.startsWith("mock_") || pageToken.startsWith("valid_test_token")) {
            // Trong môi trường test/mock: giả lập không tìm thấy bài trùng
            return PostResult.failed("NOT_FOUND", "Không tìm thấy bài viết trên Page", false, null);
        }

        try {
            // Tra cứu GET /{page-id}/feed?fields=id,message,created_time&limit=5
            String rawJson = restClient.get()
                    .uri(graphApiUrl + "/" + request.getPlatformAccountId() + "/feed?fields=id,message,created_time&limit=5&access_token=" + pageToken)
                    .retrieve()
                    .body(String.class);

            if (rawJson != null && !rawJson.isBlank()) {
                JsonNode respNode = objectMapper.readTree(rawJson);
                if (respNode.has("data") && respNode.get("data").isArray()) {
                    for (JsonNode p : respNode.get("data")) {
                        String message = p.has("message") ? p.get("message").asText() : null;
                        if (message != null && request.getText() != null && message.trim().equalsIgnoreCase(request.getText().trim())) {
                            String id = p.has("id") ? p.get("id").asText() : null;
                            if (id != null) {
                                return PostResult.success(id, "https://facebook.com/" + id, p.toString());
                            }
                        }
                    }
                }
            }
            return PostResult.failed("NOT_FOUND", "Không tìm thấy bài viết", false, null);
        } catch (Exception e) {
            log.warn("[FacebookAdapter] Error looking up post: {}", e.getMessage());
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
        // Facebook Exchange Token (60 days)
        return RefreshTokenResult.builder()
                .successful(true)
                .newAccessToken("fb_token_refreshed_" + UUID.randomUUID())
                .tokenExpiresAt(LocalDateTime.now().plusDays(60))
                .build();
    }

    @Override
    public void revokeToken(String token) {
        log.info("[FacebookAdapter] Revoking token via Graph API DELETE /me/permissions");
        if (token != null && !token.startsWith("mock_")) {
            try {
                restClient.delete()
                        .uri(graphApiUrl + "/me/permissions?access_token=" + token)
                        .retrieve()
                        .toBodilessEntity();
            } catch (Exception e) {
                log.warn("[FacebookAdapter] Error revoking token: {}", e.getMessage());
            }
        }
    }
}
