package com.nqd.nqd_tool_content.worker;

import com.nqd.nqd_tool_content.entity.User;
import com.nqd.nqd_tool_content.entity.UserSettings;
import com.nqd.nqd_tool_content.repository.UserRepository;
import com.nqd.nqd_tool_content.repository.UserSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class DailyDigestJobTest {

    @Autowired
    private DailyDigestJob dailyDigestJob;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSettingsRepository userSettingsRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = userRepository.findAll().stream().findFirst().orElseGet(() -> {
            User u = User.builder()
                    .email("digest_test@test.com")
                    .name("Digest Tester")
                    .role("ROLE_USER")
                    .build();
            return userRepository.save(u);
        });

        UserSettings settings = userSettingsRepository.findByUserId(testUser.getId()).orElseGet(() -> {
            UserSettings s = UserSettings.builder()
                    .userId(testUser.getId())
                    .dailyDigestEnabled(true)
                    .dailyDigestTime(LocalTime.MIN) // Cho phép chạy ngay trong test
                    .build();
            return userSettingsRepository.save(s);
        });
        settings.setDailyDigestEnabled(true);
        settings.setDailyDigestTime(LocalTime.MIN);
        userSettingsRepository.save(settings);
    }

    @Test
    @DisplayName("DailyDigestJob gửi báo cáo thành công và đảm bảo tính Idempotent trong ngày")
    void testDailyDigestExecutionAndIdempotency() {
        // Lần 1: Chạy thành công
        boolean firstRun = dailyDigestJob.processUserDigest(testUser);
        assertTrue(firstRun, "Lần đầu tiên chạy digest phải thành công");

        // Lần 2: Cùng một ngày không được gửi lại
        boolean secondRun = dailyDigestJob.processUserDigest(testUser);
        assertFalse(secondRun, "Chạy lại trong cùng một ngày phải bị bỏ qua để tránh trùng lặp");
    }
}
