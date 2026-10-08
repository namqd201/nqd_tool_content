package com.nqd.nqd_tool_content.worker;

import com.nqd.nqd_tool_content.entity.Notification;
import com.nqd.nqd_tool_content.entity.UserSettings;
import com.nqd.nqd_tool_content.repository.NotificationRepository;
import com.nqd.nqd_tool_content.repository.UserSettingsRepository;
import com.nqd.nqd_tool_content.service.notification.Notifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationDispatcher {

    private final NotificationRepository notificationRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final List<Notifier> notifiers;

    @Scheduled(fixedDelay = 15000) // Poll every 15 seconds
    public void dispatchPendingNotifications() {
        List<Notification> pending = notificationRepository
                .findByDeliveryStatusAndNextDeliveryAtLessThanEqual("PENDING", LocalDateTime.now());

        if (pending.isEmpty()) {
            return;
        }

        for (Notification notification : pending) {
            try {
                dispatchNotification(notification);
            } catch (Exception e) {
                log.error("Error dispatching notification {}: {}", notification.getId(), e.getMessage());
            }
        }
    }

    private void dispatchNotification(Notification notification) {
        UserSettings settings = userSettingsRepository.findByUserId(notification.getUserId()).orElse(null);
        List<String> channelsSent = new ArrayList<>();

        // 1. Telegram
        if (settings != null && settings.getNotifyTelegramChatId() != null && !settings.getNotifyTelegramChatId().isBlank()) {
            Notifier tg = findNotifier("TELEGRAM");
            if (tg != null && tg.send(settings.getNotifyTelegramChatId(), notification.getTitle(), notification.getBody())) {
                channelsSent.add("TELEGRAM");
            }
        }

        // 2. Email
        if (settings != null && settings.getNotifyEmail() != null && !settings.getNotifyEmail().isBlank()) {
            Notifier mail = findNotifier("EMAIL");
            if (mail != null && mail.send(settings.getNotifyEmail(), notification.getTitle(), notification.getBody())) {
                channelsSent.add("EMAIL");
            }
        }

        notification.setChannelsSent(String.join(",", channelsSent));
        notification.setDeliveryAttempts(notification.getDeliveryAttempts() + 1);

        if (!channelsSent.isEmpty() || (settings == null || (isEmpty(settings.getNotifyTelegramChatId()) && isEmpty(settings.getNotifyEmail())))) {
            // Success (or user hasn't configured external channels, in-app is stored)
            notification.setDeliveryStatus("SENT");
        } else {
            // Failed external deliveries
            if (notification.getDeliveryAttempts() >= 5) {
                notification.setDeliveryStatus("FAILED");
            } else {
                notification.setNextDeliveryAt(LocalDateTime.now().plusMinutes(2));
            }
        }

        notificationRepository.save(notification);
    }

    private Notifier findNotifier(String channelName) {
        return notifiers.stream()
                .filter(n -> n.getChannelName().equalsIgnoreCase(channelName))
                .findFirst()
                .orElse(null);
    }

    private boolean isEmpty(String str) {
        return str == null || str.isBlank();
    }
}
