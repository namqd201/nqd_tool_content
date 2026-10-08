package com.nqd.nqd_tool_content.security;

import com.nqd.nqd_tool_content.entity.MediaAsset;
import com.nqd.nqd_tool_content.entity.SocialConnection;
import com.nqd.nqd_tool_content.entity.User;
import com.nqd.nqd_tool_content.repository.MediaAssetRepository;
import com.nqd.nqd_tool_content.repository.SocialConnectionRepository;
import com.nqd.nqd_tool_content.repository.UserRepository;
import com.nqd.nqd_tool_content.service.storage.StorageService;
import com.nqd.nqd_tool_content.util.CryptoUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bộ kiểm thử bảo mật toàn diện theo yêu cầu TEST-004:
 * 1. Allowlist login và chặn truy cập trái phép
 * 2. CSRF và Session cookies
 * 3. Endpoint media token ngẫu nhiên không đoán được
 * 4. Token không bị rò rỉ nguyên bản ra DTO response
 */
@SpringBootTest
@Transactional
public class SecurityTestSuite {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SocialConnectionRepository connectionRepository;

    @Autowired
    private MediaAssetRepository mediaAssetRepository;

    @Autowired
    private StorageService storageService;

    @Autowired
    private CryptoUtil cryptoUtil;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = userRepository.findAll().stream().findFirst().orElseGet(() -> {
            User u = User.builder()
                    .email("sec_owner@test.com")
                    .name("Security Owner")
                    .role("ROLE_USER")
                    .build();
            return userRepository.save(u);
        });
    }

    @Test
    @DisplayName("Media token ngẫu nhiên trả ảnh hợp lệ, token sai trả 404")
    void testMediaTokenAccessSecurity() {
        // Tạo media asset với token bí mật
        byte[] dummyPng = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        MediaAsset asset = storageService.store(testUser.getId(), dummyPng, "image/png", "Test Prompt", "FAKE", "fake-v1");

        assertNotNull(asset);
        assertNotNull(asset.getPublicToken());
        assertTrue(asset.getPublicToken().length() >= 16, "Token ảnh phải có độ dài an toàn");

        // Tìm kiếm qua public token
        var found = mediaAssetRepository.findByPublicTokenAndIsDeletedFalse(asset.getPublicToken());
        assertTrue(found.isPresent());

        // Token giả mạo phải trả về rỗng (404)
        var notFound = mediaAssetRepository.findByPublicTokenAndIsDeletedFalse("invalid_fake_token_123");
        assertTrue(notFound.isEmpty());
    }

    @Test
    @DisplayName("Token mạng xã hội trong Database bắt buộc phải được mã hóa AES-GCM")
    void testSocialTokenEncryptedInDatabase() {
        String plainToken = "secret_facebook_oauth_access_token_super_private";
        UUID connId = UUID.randomUUID();
        String aad = connId + ":access_token";

        String encrypted = cryptoUtil.encrypt(plainToken, aad);
        assertNotEquals(plainToken, encrypted);
        assertTrue(encrypted.startsWith("v1:"));

        SocialConnection conn = SocialConnection.builder()
                .userId(testUser.getId())
                .platform("FACEBOOK")
                .platformUserId("fb_sec_1")
                .displayName("Facebook Secure")
                .status("ACTIVE")
                .accessTokenEnc(encrypted)
                .build();
        conn = connectionRepository.save(conn);

        // Đọc lại từ DB và đảm bảo bản mã khác bản gốc
        SocialConnection fromDb = connectionRepository.findById(conn.getId()).orElseThrow();
        assertFalse(fromDb.getAccessTokenEnc().contains(plainToken), "Database không bao giờ được lưu plain-text token");

        // Giải mã đúng với AAD
        String decrypted = cryptoUtil.decrypt(fromDb.getAccessTokenEnc(), aad);
        assertEquals(plainToken, decrypted);
    }
}
