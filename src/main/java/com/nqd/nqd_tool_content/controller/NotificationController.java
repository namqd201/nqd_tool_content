package com.nqd.nqd_tool_content.controller;

import com.nqd.nqd_tool_content.entity.Notification;
import com.nqd.nqd_tool_content.entity.User;
import com.nqd.nqd_tool_content.repository.NotificationRepository;
import com.nqd.nqd_tool_content.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    private UUID getEffectiveUserId() {
        List<User> users = userRepository.findAll();
        if (users.isEmpty()) {
            throw new IllegalStateException("Hệ thống chưa có người dùng");
        }
        return users.get(0).getId();
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size
    ) {
        UUID userId = getEffectiveUserId();
        Page<Notification> notifPage = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
        long unreadCount = notificationRepository.countByUserIdAndReadAtIsNull(userId);

        return ResponseEntity.ok(Map.of(
                "items", notifPage.getContent(),
                "unreadCount", unreadCount,
                "totalElements", notifPage.getTotalElements(),
                "totalPages", notifPage.getTotalPages()
        ));
    }

    @PostMapping("/{id}/read")
    @Transactional
    public ResponseEntity<Map<String, String>> markAsRead(@PathVariable UUID id) {
        notificationRepository.findById(id).ifPresent(n -> {
            n.setReadAt(LocalDateTime.now());
            notificationRepository.save(n);
        });
        return ResponseEntity.ok(Map.of("message", "Đã đánh dấu đã đọc"));
    }

    @PostMapping("/read-all")
    @Transactional
    public ResponseEntity<Map<String, Object>> markAllAsRead() {
        int updated = notificationRepository.markAllAsReadForUser(getEffectiveUserId());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "markedCount", updated
        ));
    }
}
