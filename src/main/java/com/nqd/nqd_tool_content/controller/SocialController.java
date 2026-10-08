package com.nqd.nqd_tool_content.controller;

import com.nqd.nqd_tool_content.dto.request.AccountToggleRequest;
import com.nqd.nqd_tool_content.dto.request.FakeConnectRequest;
import com.nqd.nqd_tool_content.dto.response.SocialAccountResponse;
import com.nqd.nqd_tool_content.dto.response.SocialConnectionResponse;
import com.nqd.nqd_tool_content.entity.User;
import com.nqd.nqd_tool_content.repository.UserRepository;
import com.nqd.nqd_tool_content.service.SocialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/social")
@RequiredArgsConstructor
public class SocialController {

    private final SocialService socialService;
    private final UserRepository userRepository;

    private UUID getEffectiveUserId() {
        List<User> users = userRepository.findAll();
        if (users.isEmpty()) {
            throw new IllegalStateException("Hệ thống chưa có người dùng");
        }
        return users.get(0).getId();
    }

    @GetMapping("/connections")
    public ResponseEntity<List<SocialConnectionResponse>> getConnections() {
        return ResponseEntity.ok(socialService.getConnections(getEffectiveUserId()));
    }

    @GetMapping("/accounts")
    public ResponseEntity<List<SocialAccountResponse>> getAccounts() {
        return ResponseEntity.ok(socialService.getAccounts(getEffectiveUserId()));
    }

    @PutMapping("/accounts/{id}/toggle")
    public ResponseEntity<SocialAccountResponse> toggleAccount(
            @PathVariable UUID id,
            @Valid @RequestBody AccountToggleRequest request) {
        return ResponseEntity.ok(socialService.toggleAccount(getEffectiveUserId(), id, request.getEnabled()));
    }

    @PostMapping("/connections/fake/connect")
    public ResponseEntity<SocialConnectionResponse> connectFake(
            @Valid @RequestBody FakeConnectRequest request) {
        return ResponseEntity.ok(socialService.connectFake(getEffectiveUserId(), request));
    }

    @PostMapping("/connections/manual/connect")
    public ResponseEntity<SocialConnectionResponse> connectManual(
            @Valid @RequestBody com.nqd.nqd_tool_content.dto.request.ManualConnectRequest request) {
        return ResponseEntity.ok(socialService.connectManual(getEffectiveUserId(), request));
    }


    @PostMapping("/connections/{id}/health-check")
    public ResponseEntity<Map<String, Object>> healthCheck(@PathVariable UUID id) {
        boolean valid = socialService.healthCheck(getEffectiveUserId(), id);
        return ResponseEntity.ok(Map.of(
                "valid", valid,
                "message", valid ? "Kết nối và token hoạt động bình thường." : "Token không hợp lệ hoặc đã bị thu hồi."
        ));
    }

    @PostMapping("/connections/{id}/disconnect")
    public ResponseEntity<Map<String, Object>> disconnect(@PathVariable UUID id) {
        socialService.disconnect(getEffectiveUserId(), id);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Đã ngắt kết nối mạng xã hội thành công."
        ));
    }
}
