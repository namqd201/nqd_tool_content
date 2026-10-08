package com.nqd.nqd_tool_content.service;

import com.nqd.nqd_tool_content.dto.request.FakeConnectRequest;
import com.nqd.nqd_tool_content.dto.response.SocialAccountResponse;
import com.nqd.nqd_tool_content.dto.response.SocialConnectionResponse;
import com.nqd.nqd_tool_content.entity.SocialAccount;
import com.nqd.nqd_tool_content.entity.SocialConnection;
import com.nqd.nqd_tool_content.entity.User;
import com.nqd.nqd_tool_content.repository.SocialAccountRepository;
import com.nqd.nqd_tool_content.repository.SocialConnectionRepository;
import com.nqd.nqd_tool_content.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class SocialServiceTest {

    @Autowired
    private SocialService socialService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SocialConnectionRepository connectionRepository;

    @Autowired
    private SocialAccountRepository accountRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(User.builder()
                .email("social_owner_" + UUID.randomUUID() + "@test.com")
                .name("App Owner")
                .role("ROLE_USER")
                .build());
    }

    @Test
    void testConnectFakeAndToggleAccount() {
        // 1. Connect fake Facebook
        FakeConnectRequest req = FakeConnectRequest.builder()
                .platform("FACEBOOK")
                .displayName("Facebook Demo Fanpage")
                .build();

        SocialConnectionResponse connRes = socialService.connectFake(testUser.getId(), req);
        assertNotNull(connRes);
        assertEquals("FACEBOOK", connRes.getPlatform());
        assertFalse(connRes.getAccounts().isEmpty());

        SocialAccountResponse account = connRes.getAccounts().get(0);
        assertTrue(account.getIsEnabled());

        // 2. Toggle disable
        SocialAccountResponse toggled = socialService.toggleAccount(testUser.getId(), UUID.fromString(account.getId()), false);
        assertFalse(toggled.getIsEnabled());

        // 3. Health check
        boolean healthy = socialService.healthCheck(testUser.getId(), UUID.fromString(connRes.getId()));
        assertTrue(healthy);

        // 4. Disconnect
        socialService.disconnect(testUser.getId(), UUID.fromString(connRes.getId()));
        
        List<SocialConnectionResponse> afterDisconnect = socialService.getConnections(testUser.getId());
        assertTrue(afterDisconnect.isEmpty());
    }
}
