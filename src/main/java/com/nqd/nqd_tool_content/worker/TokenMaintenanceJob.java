package com.nqd.nqd_tool_content.worker;

import com.nqd.nqd_tool_content.entity.SocialConnection;
import com.nqd.nqd_tool_content.repository.SocialConnectionRepository;
import com.nqd.nqd_tool_content.service.NotificationService;
import com.nqd.nqd_tool_content.service.social.SocialAdapterRegistry;
import com.nqd.nqd_tool_content.service.social.SocialPlatformAdapter;
import com.nqd.nqd_tool_content.service.social.dto.RefreshTokenResult;
import com.nqd.nqd_tool_content.util.CryptoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenMaintenanceJob {

    private final SocialConnectionRepository connectionRepository;
    private final SocialAdapterRegistry adapterRegistry;
    private final NotificationService notificationService;
    private final CryptoUtil cryptoUtil;

    /**
     * Chạy định kỳ mỗi 30 phút để kiểm tra và bảo trì OAuth token của các mạng xã hội.
     */
    @Scheduled(fixedDelay = 1800000, initialDelay = 60000) // 30 phút
    @Transactional
    public void maintainTokens() {
        log.info("[TokenMaintenanceJob] Starting OAuth token check & refresh job...");
        LocalDateTime threshold = LocalDateTime.now().plusDays(7); // Cảnh báo hoặc refresh trước 7 ngày

        List<SocialConnection> expiringSoon = connectionRepository
                .findByStatusAndTokenExpiresAtLessThanEqualAndIsDeletedFalse("ACTIVE", threshold);

        for (SocialConnection conn : expiringSoon) {
            try {
                processConnection(conn);
            } catch (Exception e) {
                log.error("[TokenMaintenanceJob] Error processing connection id={}", conn.getId(), e);
            }
        }
    }

    private void processConnection(SocialConnection conn) {
        LocalDateTime now = LocalDateTime.now();

        // 1. Kiểm tra nếu đã quá hạn
        if (conn.getTokenExpiresAt() != null && conn.getTokenExpiresAt().isBefore(now)) {
            conn.setStatus("EXPIRED");
            conn.setLastError("Token đã hết hạn và cần được kết nối lại.");
            connectionRepository.save(conn);

            notificationService.createNotification(
                    conn.getUserId(),
                    "TOKEN_REVOKED",
                    "Token " + conn.getPlatform() + " đã hết hạn",
                    "Kết nối " + conn.getDisplayName() + " đã hết hạn sử dụng. Vui lòng vào trang Kênh để kết nối lại.",
                    "/channels"
            );
            return;
        }

        // 2. Thử refresh nếu có refresh token
        if (conn.getRefreshTokenEnc() != null) {
            Optional<SocialPlatformAdapter> adapterOpt = adapterRegistry.getAdapter(conn.getPlatform());
            if (adapterOpt.isPresent()) {
                SocialPlatformAdapter adapter = adapterOpt.get();
                try {
                    String aad = conn.getId() + ":refresh_token";
                    String decryptedRefreshToken = cryptoUtil.decrypt(conn.getRefreshTokenEnc(), aad);

                    RefreshTokenResult result = adapter.refreshToken(decryptedRefreshToken);
                    if (result.isSuccessful()) {
                        String accessAad = conn.getId() + ":access_token";
                        conn.setAccessTokenEnc(cryptoUtil.encrypt(result.getNewAccessToken(), accessAad));
                        if (result.getNewRefreshToken() != null) {
                            conn.setRefreshTokenEnc(cryptoUtil.encrypt(result.getNewRefreshToken(), aad));
                        }
                        conn.setTokenExpiresAt(result.getTokenExpiresAt());
                        conn.setRefreshTokenExpiresAt(result.getRefreshTokenExpiresAt());
                        conn.setLastRefreshedAt(LocalDateTime.now());
                        conn.setRefreshFailureCount(0);
                        conn.setLastError(null);
                        connectionRepository.save(conn);
                        log.info("[TokenMaintenanceJob] Refreshed token successfully for connection {}", conn.getId());
                        return;
                    } else {
                        conn.setRefreshFailureCount(conn.getRefreshFailureCount() + 1);
                        conn.setLastError(result.getErrorMessage());
                    }
                } catch (Exception e) {
                    conn.setRefreshFailureCount(conn.getRefreshFailureCount() + 1);
                    conn.setLastError(e.getMessage());
                }
            }
        }

        // 3. Nếu không thể refresh tự động và sắp hết hạn (< 7 ngày), gửi thông báo cảnh báo
        if (conn.getTokenExpiresAt() != null && conn.getTokenExpiresAt().isBefore(now.plusDays(7))) {
            long daysLeft = java.time.Duration.between(now, conn.getTokenExpiresAt()).toDays();
            notificationService.createNotification(
                    conn.getUserId(),
                    "TOKEN_EXPIRING",
                    "Token " + conn.getPlatform() + " sắp hết hạn (" + Math.max(0, daysLeft) + " ngày)",
                    "Kết nối " + conn.getDisplayName() + " sắp hết hạn. Vui lòng kết nối lại để đảm bảo bài viết tự động không bị gián đoạn.",
                    "/channels"
            );
        }

        connectionRepository.save(conn);
    }
}
