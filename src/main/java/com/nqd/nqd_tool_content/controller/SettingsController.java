package com.nqd.nqd_tool_content.controller;

import com.nqd.nqd_tool_content.entity.User;
import com.nqd.nqd_tool_content.entity.UserSettings;
import com.nqd.nqd_tool_content.repository.UserRepository;
import com.nqd.nqd_tool_content.service.NotificationService;
import com.nqd.nqd_tool_content.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    private UUID getEffectiveUserId() {
        List<User> users = userRepository.findAll();
        if (users.isEmpty()) {
            throw new IllegalStateException("Hệ thống chưa có người dùng");
        }
        return users.get(0).getId();
    }

    @GetMapping
    public ResponseEntity<UserSettings> getSettings() {
        return ResponseEntity.ok(settingsService.getOrCreateSettings(getEffectiveUserId()));
    }

    @PutMapping
    public ResponseEntity<UserSettings> updateSettings(@RequestBody UserSettings newSettings) {
        return ResponseEntity.ok(settingsService.updateSettings(getEffectiveUserId(), newSettings));
    }

    @GetMapping("/ai/providers")
    public ResponseEntity<List<Map<String, Object>>> getAiProviders() {
        return ResponseEntity.ok(settingsService.getAvailableAiProviders());
    }

    @GetMapping("/ai/pipeline")
    public ResponseEntity<com.nqd.nqd_tool_content.dto.response.AiPipelineProgressResponse> getAiPipelineProgress() {
        return ResponseEntity.ok(settingsService.getAiPipelineProgress(getEffectiveUserId()));
    }

    @PostMapping("/notifications/test")
    public ResponseEntity<Map<String, Object>> testNotification(@RequestBody Map<String, String> request) {
        String channel = request.getOrDefault("channel", "TELEGRAM");
        boolean sent = notificationService.sendTestNotification(getEffectiveUserId(), channel);
        return ResponseEntity.ok(Map.of(
                "success", sent,
                "channel", channel,
                "message", sent ? "Đã gửi thông báo thử nghiệm thành công" : "Không thể gửi (kiểm tra cấu hình token/chat ID)"
        ));
    }
}
