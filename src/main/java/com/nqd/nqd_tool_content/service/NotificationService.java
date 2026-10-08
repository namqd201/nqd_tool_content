package com.nqd.nqd_tool_content.service;

import com.nqd.nqd_tool_content.entity.Notification;
import com.nqd.nqd_tool_content.entity.UserSettings;
import com.nqd.nqd_tool_content.repository.NotificationRepository;
import com.nqd.nqd_tool_content.repository.UserSettingsRepository;
import com.nqd.nqd_tool_content.service.notification.Notifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final List<Notifier> notifiers;

    @Transactional
    public Notification createNotification(UUID userId, String type, String title, String body, String link) {
        return enqueue(userId, type, title, body, null, null);
    }

    /**
     * Enqueue a notification inside business transaction (Outbox pattern).
     */
    @Transactional
    public Notification enqueue(UUID userId, String type, String title, String body, UUID relatedPostId, UUID relatedPlanId) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .body(body)
                .relatedPostId(relatedPostId)
                .relatedPlanId(relatedPlanId)
                .deliveryStatus("PENDING")
                .deliveryAttempts(0)
                .nextDeliveryAt(LocalDateTime.now())
                .build();

        return notificationRepository.save(notification);
    }

    /**
     * Send test notification via Telegram / Email directly.
     */
    public boolean sendTestNotification(UUID userId, String channel) {
        UserSettings settings = userSettingsRepository.findByUserId(userId).orElse(null);
        String title = "Thử nghiệm thông báo NQDSMTool";
        String body = "Đây là thông báo kiểm tra kết nối từ hệ thống NQDSMTool.";

        for (Notifier notifier : notifiers) {
            if (notifier.getChannelName().equalsIgnoreCase(channel)) {
                String recipient = null;
                if ("TELEGRAM".equalsIgnoreCase(channel) && settings != null) {
                    recipient = settings.getNotifyTelegramChatId();
                } else if ("EMAIL".equalsIgnoreCase(channel) && settings != null) {
                    recipient = settings.getNotifyEmail();
                }
                return notifier.send(recipient, title, body);
            }
        }
        return false;
    }
}
