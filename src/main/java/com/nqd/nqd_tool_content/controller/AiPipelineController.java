package com.nqd.nqd_tool_content.controller;

import com.nqd.nqd_tool_content.dto.response.AiPipelineProgressResponse;
import com.nqd.nqd_tool_content.entity.User;
import com.nqd.nqd_tool_content.repository.UserRepository;
import com.nqd.nqd_tool_content.service.SettingsService;
import com.nqd.nqd_tool_content.worker.ContentGenerationWorker;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiPipelineController {

    private final SettingsService settingsService;
    private final ContentGenerationWorker contentGenerationWorker;
    private final UserRepository userRepository;

    private UUID getEffectiveUserId() {
        List<User> users = userRepository.findAll();
        if (users.isEmpty()) {
            throw new IllegalStateException("Hệ thống chưa có người dùng");
        }
        return users.get(0).getId();
    }

    @GetMapping("/pipeline")
    public ResponseEntity<AiPipelineProgressResponse> getPipelineProgress() {
        return ResponseEntity.ok(settingsService.getAiPipelineProgress(getEffectiveUserId()));
    }

    @PostMapping("/trigger")
    public ResponseEntity<Map<String, Object>> triggerAiGeneration() {
        UUID userId = getEffectiveUserId();
        int triggeredCount = contentGenerationWorker.triggerNowForUser(userId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "triggeredCount", triggeredCount,
                "message", triggeredCount > 0 
                        ? "Đã kích hoạt AI tiến hành viết " + triggeredCount + " bài đăng ngay lập tức!" 
                        : "Không có bài đăng nào đang chờ duyệt sinh bài trong hàng đợi."
        ));
    }
}
