package com.nqd.nqd_tool_content.controller;

import com.nqd.nqd_tool_content.dto.response.ApiResponse;
import com.nqd.nqd_tool_content.dto.request.PostTemplateRequest;
import com.nqd.nqd_tool_content.dto.response.PostTemplateResponse;
import com.nqd.nqd_tool_content.entity.User;
import com.nqd.nqd_tool_content.repository.UserRepository;
import com.nqd.nqd_tool_content.service.PostTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
public class PostTemplateController {

    private final PostTemplateService templateService;
    private final UserRepository userRepository;

    private UUID getEffectiveUserId() {
        List<User> users = userRepository.findAll();
        if (users.isEmpty()) {
            throw new IllegalStateException("Hệ thống chưa có người dùng");
        }
        return users.get(0).getId();
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PostTemplateResponse>>> listTemplates() {
        UUID userId = getEffectiveUserId();
        return ResponseEntity.ok(ApiResponse.success(templateService.listTemplates(userId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PostTemplateResponse>> getTemplate(@PathVariable UUID id) {
        UUID userId = getEffectiveUserId();
        return ResponseEntity.ok(ApiResponse.success(templateService.getTemplate(userId, id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PostTemplateResponse>> createTemplate(@Valid @RequestBody PostTemplateRequest req) {
        UUID userId = getEffectiveUserId();
        return ResponseEntity.ok(ApiResponse.success(templateService.createTemplate(userId, req)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PostTemplateResponse>> updateTemplate(
            @PathVariable UUID id,
            @Valid @RequestBody PostTemplateRequest req
    ) {
        UUID userId = getEffectiveUserId();
        return ResponseEntity.ok(ApiResponse.success(templateService.updateTemplate(userId, id, req)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(@PathVariable UUID id) {
        UUID userId = getEffectiveUserId();
        templateService.deleteTemplate(userId, id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{id}/set-default")
    public ResponseEntity<ApiResponse<PostTemplateResponse>> setDefaultTemplate(@PathVariable UUID id) {
        UUID userId = getEffectiveUserId();
        return ResponseEntity.ok(ApiResponse.success(templateService.setDefault(userId, id)));
    }
}
