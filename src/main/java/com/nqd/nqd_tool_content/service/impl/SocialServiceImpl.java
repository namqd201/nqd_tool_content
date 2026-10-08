package com.nqd.nqd_tool_content.service.impl;

import com.nqd.nqd_tool_content.dto.request.FakeConnectRequest;
import com.nqd.nqd_tool_content.dto.response.SocialAccountResponse;
import com.nqd.nqd_tool_content.dto.response.SocialConnectionResponse;
import com.nqd.nqd_tool_content.entity.SocialAccount;
import com.nqd.nqd_tool_content.entity.SocialConnection;
import com.nqd.nqd_tool_content.exception.ResourceNotFoundException;
import com.nqd.nqd_tool_content.repository.SocialAccountRepository;
import com.nqd.nqd_tool_content.repository.SocialConnectionRepository;
import com.nqd.nqd_tool_content.service.AuditService;
import com.nqd.nqd_tool_content.service.SocialService;
import com.nqd.nqd_tool_content.service.social.SocialAdapterRegistry;
import com.nqd.nqd_tool_content.service.social.SocialPlatformAdapter;
import com.nqd.nqd_tool_content.service.social.dto.DiscoveredAccount;
import com.nqd.nqd_tool_content.util.CryptoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SocialServiceImpl implements SocialService {

    private final SocialConnectionRepository connectionRepository;
    private final SocialAccountRepository accountRepository;
    private final SocialAdapterRegistry adapterRegistry;
    private final CryptoUtil cryptoUtil;
    private final AuditService auditService;

    @Override
    @Transactional(readOnly = true)
    public List<SocialConnectionResponse> getConnections(UUID userId) {
        List<SocialConnection> connections = connectionRepository.findByUserIdAndIsDeletedFalse(userId);
        List<SocialAccount> accounts = accountRepository.findByUserIdAndIsDeletedFalse(userId);

        Map<UUID, List<SocialAccount>> accountsByConnection = accounts.stream()
                .collect(Collectors.groupingBy(SocialAccount::getConnectionId));

        return connections.stream()
                .map(conn -> toConnectionResponse(conn, accountsByConnection.getOrDefault(conn.getId(), List.of())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SocialAccountResponse> getAccounts(UUID userId) {
        return accountRepository.findByUserIdAndIsDeletedFalse(userId).stream()
                .map(this::toAccountResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SocialAccountResponse toggleAccount(UUID userId, UUID accountId, boolean enabled) {
        SocialAccount account = accountRepository.findById(accountId)
                .filter(a -> a.getUserId().equals(userId) && !Boolean.TRUE.equals(a.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản mạng xã hội không tồn tại"));

        account.setIsEnabled(enabled);
        accountRepository.save(account);

        auditService.log(userId, "TOGGLE_ACCOUNT", "SocialAccount", account.getId().toString(),
                "Tài khoản " + account.getDisplayName() + " đã được " + (enabled ? "bật" : "tắt"));

        return toAccountResponse(account);
    }

    @Override
    @Transactional
    public SocialConnectionResponse connectFake(UUID userId, FakeConnectRequest request) {
        String platform = request.getPlatform().toUpperCase();
        String displayName = request.getDisplayName() != null ? request.getDisplayName() : (platform + " Account Demo");

        // 1. Kiểm tra xem connection đã tồn tại chưa
        List<SocialConnection> existing = connectionRepository.findByUserIdAndIsDeletedFalse(userId);
        for (SocialConnection c : existing) {
            if (c.getPlatform().equalsIgnoreCase(platform)) {
                // Đánh dấu xóa mềm để thay thế
                c.setIsDeleted(true);
                c.setDeletedAt(LocalDateTime.now());
                connectionRepository.save(c);
            }
        }

        String mockToken = "mock_token_" + UUID.randomUUID();

        SocialConnection connection = SocialConnection.builder()
                .userId(userId)
                .platform(platform)
                .platformUserId("mock_user_" + UUID.randomUUID().toString().substring(0, 8))
                .displayName(displayName)
                .scopes("read,write,publish")
                .status("ACTIVE")
                .accessTokenEnc("pending_init")
                .keyVersion(cryptoUtil.getActiveKeyVersion())
                .tokenExpiresAt(LocalDateTime.now().plusDays(60))
                .connectedAt(LocalDateTime.now())
                .build();
        connection = connectionRepository.saveAndFlush(connection);

        String aad = connection.getId() + ":access_token";
        connection.setAccessTokenEnc(cryptoUtil.encrypt(mockToken, aad));
        connection = connectionRepository.save(connection);

        // 2. Discover accounts qua adapter
        SocialPlatformAdapter adapter = adapterRegistry.getAdapter(platform)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy adapter cho platform: " + platform));

        List<DiscoveredAccount> discovered = adapter.discoverAccounts(mockToken);
        List<SocialAccount> savedAccounts = new ArrayList<>();

        for (DiscoveredAccount da : discovered) {
            String pageTokenEnc = null;
            if (da.getPageAccessToken() != null) {
                pageTokenEnc = cryptoUtil.encrypt(da.getPageAccessToken(), connection.getId() + ":page_access_token");
            }

            SocialAccount account = SocialAccount.builder()
                    .userId(userId)
                    .connectionId(connection.getId())
                    .platform(platform)
                    .accountType(da.getAccountType() != null ? da.getAccountType() : "PAGE")
                    .platformAccountId(da.getPlatformAccountId())
                    .displayName(da.getDisplayName())
                    .username(da.getUsername())
                    .avatarUrl(da.getAvatarUrl())
                    .isEnabled(true)
                    .status("ACTIVE")
                    .pageAccessTokenEnc(pageTokenEnc)
                    .keyVersion(cryptoUtil.getActiveKeyVersion())
                    .lastHealthCheckAt(LocalDateTime.now())
                    .build();
            savedAccounts.add(accountRepository.save(account));
        }

        auditService.log(userId, "CONNECT_SOCIAL", "SocialConnection", connection.getId().toString(),
                "Kết nối thành công kênh " + platform + " (" + displayName + ")");

        return toConnectionResponse(connection, savedAccounts);
    }

    @Override
    @Transactional
    public SocialConnectionResponse connectManual(UUID userId, com.nqd.nqd_tool_content.dto.request.ManualConnectRequest request) {
        String platform = request.getPlatform().toUpperCase();
        String rawToken = request.getAccessToken() != null && !request.getAccessToken().isBlank()
                ? request.getAccessToken().trim()
                : "manual_token_" + UUID.randomUUID();

        SocialPlatformAdapter adapter = adapterRegistry.getAdapter(platform).orElse(null);

        // Nếu là Facebook và người dùng cung cấp App ID + App Secret, tự động đổi sang Long-Lived Token (60 ngày)
        // Từ Long-Lived Token này, khi quét Page qua /me/accounts sẽ sinh ra Page Access Token VĨNH VIỄN (Never Expire).
        if ("FACEBOOK".equalsIgnoreCase(platform) && adapter instanceof com.nqd.nqd_tool_content.service.social.adapter.FacebookAdapter fbAdapter) {
            if (request.getAppId() != null && !request.getAppId().isBlank() &&
                request.getAppSecret() != null && !request.getAppSecret().isBlank()) {
                rawToken = fbAdapter.exchangeForLongLivedToken(rawToken, request.getAppId(), request.getAppSecret());
            }
        }

        // 1. Quản lý SocialConnection: tìm kết nối đang hoạt động của platform này hoặc tạo mới
        List<SocialConnection> existingConns = connectionRepository.findByUserIdAndIsDeletedFalse(userId);
        SocialConnection connection = null;
        for (SocialConnection c : existingConns) {
            if (c.getPlatform().equalsIgnoreCase(platform)) {
                connection = c;
                break;
            }
        }

        if (connection == null) {
            String connName = request.getDisplayName() != null && !request.getDisplayName().isBlank()
                    ? request.getDisplayName().trim()
                    : (platform + " Connection");
            connection = SocialConnection.builder()
                    .userId(userId)
                    .platform(platform)
                    .platformUserId("user_" + UUID.randomUUID().toString().substring(0, 8))
                    .displayName(connName)
                    .scopes("pages_show_list,pages_manage_posts,pages_read_engagement")
                    .status("ACTIVE")
                    .accessTokenEnc("pending")
                    .keyVersion(cryptoUtil.getActiveKeyVersion())
                    .tokenExpiresAt(LocalDateTime.now().plusYears(1))
                    .connectedAt(LocalDateTime.now())
                    .build();
            connection = connectionRepository.saveAndFlush(connection);
            connection.setAccessTokenEnc(cryptoUtil.encrypt(rawToken, connection.getId() + ":access_token"));
            connection = connectionRepository.save(connection);
        } else {
            // Cập nhật token mới cho connection nếu được cung cấp
            connection.setAccessTokenEnc(cryptoUtil.encrypt(rawToken, connection.getId() + ":access_token"));
            connection.setStatus("ACTIVE");
            connection.setTokenExpiresAt(LocalDateTime.now().plusYears(1));
            connection = connectionRepository.save(connection);
        }

        // 2. Thử tự động khám phá Page qua Adapter nếu là token thật
        List<DiscoveredAccount> discovered = new ArrayList<>();
        if (adapter != null && !rawToken.startsWith("manual_") && !rawToken.startsWith("mock_")) {
            try {
                discovered = adapter.discoverAccounts(rawToken);
            } catch (com.nqd.nqd_tool_content.exception.BusinessException be) {
                throw be;
            } catch (Exception e) {
                log.warn("Lỗi khi tự động lấy danh sách Page qua Graph API: {}", e.getMessage());
            }
        }

        List<SocialAccount> savedAccounts = new ArrayList<>();

        if (!discovered.isEmpty()) {
            // Lưu các Page tự động tìm thấy
            for (DiscoveredAccount da : discovered) {
                String pageToken = da.getPageAccessToken() != null ? da.getPageAccessToken() : rawToken;
                String pageTokenEnc = cryptoUtil.encrypt(pageToken, connection.getId() + ":page_access_token");

                // Tìm xem account này đã tồn tại trong DB chưa
                SocialAccount account = accountRepository.findByUserIdAndIsDeletedFalse(userId).stream()
                        .filter(a -> a.getPlatform().equalsIgnoreCase(platform) && a.getPlatformAccountId().equals(da.getPlatformAccountId()))
                        .findFirst()
                        .orElse(null);

                if (account == null) {
                    account = SocialAccount.builder()
                            .userId(userId)
                            .connectionId(connection.getId())
                            .platform(platform)
                            .accountType(da.getAccountType() != null ? da.getAccountType() : "PAGE")
                            .platformAccountId(da.getPlatformAccountId())
                            .displayName(da.getDisplayName())
                            .username(da.getUsername())
                            .avatarUrl(da.getAvatarUrl())
                            .isEnabled(true)
                            .status("ACTIVE")
                            .pageAccessTokenEnc(pageTokenEnc)
                            .keyVersion(cryptoUtil.getActiveKeyVersion())
                            .lastHealthCheckAt(LocalDateTime.now())
                            .build();
                } else {
                    account.setDisplayName(da.getDisplayName());
                    account.setPageAccessTokenEnc(pageTokenEnc);
                    account.setStatus("ACTIVE");
                    account.setIsEnabled(true);
                    account.setAvatarUrl(da.getAvatarUrl());
                }
                savedAccounts.add(accountRepository.save(account));
            }
        } else {
            // Người dùng nhập trực tiếp thông tin Page thủ công
            String pageId = request.getPlatformAccountId() != null && !request.getPlatformAccountId().isBlank()
                    ? request.getPlatformAccountId().trim()
                    : "page_" + UUID.randomUUID().toString().substring(0, 8);

            String pageName = request.getDisplayName() != null && !request.getDisplayName().isBlank()
                    ? request.getDisplayName().trim()
                    : (platform + " Fanpage (" + pageId + ")");

            String pageTokenEnc = cryptoUtil.encrypt(rawToken, connection.getId() + ":page_access_token");

            SocialAccount account = accountRepository.findByUserIdAndIsDeletedFalse(userId).stream()
                    .filter(a -> a.getPlatform().equalsIgnoreCase(platform) && a.getPlatformAccountId().equals(pageId))
                    .findFirst()
                    .orElse(null);

            if (account == null) {
                account = SocialAccount.builder()
                        .userId(userId)
                        .connectionId(connection.getId())
                        .platform(platform)
                        .accountType(request.getAccountType() != null ? request.getAccountType() : "PAGE")
                        .platformAccountId(pageId)
                        .displayName(pageName)
                        .username(request.getUsername())
                        .avatarUrl(request.getAvatarUrl() != null ? request.getAvatarUrl() : "https://images.unsplash.com/photo-1544717305-2782549b5136?w=120&auto=format&fit=crop&q=80")
                        .isEnabled(true)
                        .status("ACTIVE")
                        .pageAccessTokenEnc(pageTokenEnc)
                        .keyVersion(cryptoUtil.getActiveKeyVersion())
                        .lastHealthCheckAt(LocalDateTime.now())
                        .build();
            } else {
                account.setDisplayName(pageName);
                account.setPageAccessTokenEnc(pageTokenEnc);
                account.setStatus("ACTIVE");
                account.setIsEnabled(true);
            }
            savedAccounts.add(accountRepository.save(account));
        }

        auditService.log(userId, "CONNECT_MANUAL", "SocialConnection", connection.getId().toString(),
                "Thêm kênh thành công bằng cấu hình thủ công: " + platform);

        // Lấy lại danh sách toàn bộ accounts của connection này
        List<SocialAccount> allAccounts = accountRepository.findByConnectionIdAndIsDeletedFalse(connection.getId());
        return toConnectionResponse(connection, allAccounts);
    }

    @Override
    @Transactional
    public boolean healthCheck(UUID userId, UUID connectionId) {
        SocialConnection connection = connectionRepository.findById(connectionId)
                .filter(c -> c.getUserId().equals(userId) && !Boolean.TRUE.equals(c.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Kết nối mạng xã hội không tồn tại"));

        SocialPlatformAdapter adapter = adapterRegistry.getAdapter(connection.getPlatform())
                .orElseThrow(() -> new IllegalStateException("Adapter không tồn tại: " + connection.getPlatform()));

        boolean isValid = false;
        try {
            String aad = connection.getId() + ":access_token";
            String decryptedToken = cryptoUtil.decrypt(connection.getAccessTokenEnc(), aad);
            isValid = adapter.validateToken(decryptedToken);
        } catch (Exception e) {
            log.error("Error validating token for connection: {}", connectionId, e);
            isValid = false;
        }

        if (isValid) {
            connection.setStatus("ACTIVE");
            connection.setLastError(null);
        } else {
            connection.setStatus("REVOKED");
            connection.setLastError("Token không hợp lệ hoặc đã bị hủy quyền.");
        }
        connection.setLastRefreshedAt(LocalDateTime.now());
        connectionRepository.save(connection);

        // Update accounts health check timestamp
        List<SocialAccount> accounts = accountRepository.findByConnectionIdAndIsDeletedFalse(connectionId);
        for (SocialAccount acc : accounts) {
            acc.setLastHealthCheckAt(LocalDateTime.now());
            if (!isValid) {
                acc.setStatus("REVOKED");
                acc.setNeedsReauthAt(LocalDateTime.now());
            } else {
                acc.setStatus("ACTIVE");
                acc.setNeedsReauthAt(null);
            }
            accountRepository.save(acc);
        }

        return isValid;
    }

    @Override
    @Transactional
    public void disconnect(UUID userId, UUID connectionId) {
        SocialConnection connection = connectionRepository.findById(connectionId)
                .filter(c -> c.getUserId().equals(userId) && !Boolean.TRUE.equals(c.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Kết nối mạng xã hội không tồn tại"));

        // Soft delete connection
        connection.setIsDeleted(true);
        connection.setDeletedAt(LocalDateTime.now());
        connection.setStatus("REVOKED");
        connectionRepository.save(connection);

        // Soft delete all accounts under this connection
        List<SocialAccount> accounts = accountRepository.findByConnectionIdAndIsDeletedFalse(connectionId);
        for (SocialAccount acc : accounts) {
            acc.setIsDeleted(true);
            acc.setDeletedAt(LocalDateTime.now());
            acc.setIsEnabled(false);
            accountRepository.save(acc);
        }

        auditService.log(userId, "DISCONNECT_SOCIAL", "SocialConnection", connectionId.toString(),
                "Ngắt kết nối mạng xã hội " + connection.getPlatform());
    }

    private SocialConnectionResponse toConnectionResponse(SocialConnection conn, List<SocialAccount> accounts) {
        List<SocialAccountResponse> accountResponses = accounts.stream()
                .map(this::toAccountResponse)
                .collect(Collectors.toList());

        return SocialConnectionResponse.builder()
                .id(conn.getId().toString())
                .platform(conn.getPlatform())
                .platformUserId(conn.getPlatformUserId())
                .displayName(conn.getDisplayName())
                .status(conn.getStatus())
                .scopes(conn.getScopes())
                .tokenExpiresAt(conn.getTokenExpiresAt())
                .refreshTokenExpiresAt(conn.getRefreshTokenExpiresAt())
                .lastRefreshedAt(conn.getLastRefreshedAt())
                .refreshFailureCount(conn.getRefreshFailureCount())
                .lastError(conn.getLastError())
                .connectedAt(conn.getConnectedAt())
                .accounts(accountResponses)
                .build();
    }

    private SocialAccountResponse toAccountResponse(SocialAccount acc) {
        return SocialAccountResponse.builder()
                .id(acc.getId().toString())
                .connectionId(acc.getConnectionId().toString())
                .platform(acc.getPlatform())
                .accountType(acc.getAccountType())
                .platformAccountId(acc.getPlatformAccountId())
                .displayName(acc.getDisplayName())
                .username(acc.getUsername())
                .avatarUrl(acc.getAvatarUrl())
                .isEnabled(acc.getIsEnabled())
                .status(acc.getStatus())
                .lastHealthCheckAt(acc.getLastHealthCheckAt())
                .lastError(acc.getLastError())
                .needsReauthAt(acc.getNeedsReauthAt())
                .build();
    }
}
