package com.nqd.nqd_tool_content.worker;

import com.nqd.nqd_tool_content.dto.response.ScheduleSummaryResponse;
import com.nqd.nqd_tool_content.entity.User;
import com.nqd.nqd_tool_content.entity.UserSettings;
import com.nqd.nqd_tool_content.repository.UserRepository;
import com.nqd.nqd_tool_content.repository.UserSettingsRepository;
import com.nqd.nqd_tool_content.service.NotificationService;
import com.nqd.nqd_tool_content.service.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DailyDigestJob (OPS-001):
 * - Quét và gửi thông báo tổng kết hàng ngày (Daily Digest) cho chủ sở hữu hệ thống
 * - Kiểm tra múi giờ người dùng và giờ cấu hình gửi digest (mặc định 08:00 sáng)
 * - Đảm bảo tính Idempotency: Chỉ gửi đúng 1 lần mỗi ngày kể cả khi ứng dụng restart
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyDigestJob {

    private final UserRepository userRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final PostService postService;
    private final NotificationService notificationService;

    // Lưu vết ngày đã gửi digest cho từng user để tránh gửi trùng lặp trong ngày
    private final Map<String, LocalDate> lastSentDates = new ConcurrentHashMap<>();

    /**
     * Chạy mỗi 15 phút để kiểm tra giờ gửi Daily Digest cho người dùng
     */
    @Scheduled(fixedDelay = 900000, initialDelay = 60000)
    @Transactional
    public void runDailyDigestSchedule() {
        List<User> users = userRepository.findAll();
        for (User user : users) {
            try {
                processUserDigest(user);
            } catch (Exception e) {
                log.error("[DailyDigestJob] Error generating digest for user {}: {}", user.getId(), e.getMessage(), e);
            }
        }
    }

    public boolean processUserDigest(User user) {
        UserSettings settings = userSettingsRepository.findByUserId(user.getId()).orElse(null);
        if (settings != null && Boolean.FALSE.equals(settings.getDailyDigestEnabled())) {
            return false; // Người dùng đã tắt tính năng Digest
        }

        LocalTime digestTime = (settings != null && settings.getDailyDigestTime() != null)
                ? settings.getDailyDigestTime()
                : LocalTime.of(8, 0);

        LocalTime now = LocalTime.now();
        LocalDate today = LocalDate.now();

        // Kiểm tra xem đã qua giờ cấu hình chưa và hôm nay đã gửi chưa
        if (now.isBefore(digestTime)) {
            return false;
        }

        String userKey = user.getId().toString();
        if (today.equals(lastSentDates.get(userKey))) {
            return false; // Đã gửi trong ngày hôm nay
        }

        // Tạo nội dung Digest
        ScheduleSummaryResponse summary = postService.getScheduleSummary(user.getId());

        String title = "Báo cáo tổng kết ngày " + today;
        String content = String.format(
                "Xin chào! Tổng kết hôm nay:\n" +
                "- Bài đã đăng hôm nay: %d\n" +
                "- Bài dự kiến đăng trong 24h tới: %d\n" +
                "- Bài cần bạn xử lý / chú ý: %d",
                summary.getPublishedToday(),
                summary.getScheduledNext24h(),
                summary.getNeedsAttention()
        );

        notificationService.enqueue(
                user.getId(),
                "DAILY_DIGEST",
                title,
                content,
                null,
                null
        );

        lastSentDates.put(userKey, today);
        log.info("[DailyDigestJob] Successfully dispatched DAILY_DIGEST for user {}", user.getId());
        return true;
    }
}
