package com.nqd.nqd_tool_content.controller;

import com.nqd.nqd_tool_content.dto.request.CreatePlanRequest;
import com.nqd.nqd_tool_content.dto.response.PlanPreviewResponse;
import com.nqd.nqd_tool_content.dto.response.PlanResponse;
import com.nqd.nqd_tool_content.entity.User;
import com.nqd.nqd_tool_content.repository.UserRepository;
import com.nqd.nqd_tool_content.service.PlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;
    private final UserRepository userRepository;

    private UUID getEffectiveUserId() {
        List<User> users = userRepository.findAll();
        if (users.isEmpty()) {
            throw new IllegalStateException("Hệ thống chưa có người dùng");
        }
        return users.get(0).getId();
    }

    @GetMapping
    public ResponseEntity<List<PlanResponse>> getPlans() {
        return ResponseEntity.ok(planService.getPlans(getEffectiveUserId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlanResponse> getPlan(@PathVariable UUID id) {
        return ResponseEntity.ok(planService.getPlan(getEffectiveUserId(), id));
    }

    @PostMapping("/preview")
    public ResponseEntity<PlanPreviewResponse> preview(@Valid @RequestBody CreatePlanRequest request) {
        return ResponseEntity.ok(planService.preview(request));
    }

    @PostMapping("/draft")
    public ResponseEntity<PlanResponse> createDraft(@Valid @RequestBody CreatePlanRequest request) {
        return ResponseEntity.ok(planService.createDraft(getEffectiveUserId(), request));
    }

    @PutMapping("/{id}/draft")
    public ResponseEntity<PlanResponse> updateDraft(
            @PathVariable UUID id,
            @Valid @RequestBody CreatePlanRequest request) {
        return ResponseEntity.ok(planService.updateDraft(getEffectiveUserId(), id, request));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<PlanResponse> activatePlan(@PathVariable UUID id) {
        return ResponseEntity.ok(planService.activatePlan(getEffectiveUserId(), id));
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<PlanResponse> pausePlan(@PathVariable UUID id) {
        return ResponseEntity.ok(planService.pausePlan(getEffectiveUserId(), id));
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<PlanResponse> resumePlan(@PathVariable UUID id) {
        return ResponseEntity.ok(planService.resumePlan(getEffectiveUserId(), id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Map<String, Object>> cancelPlan(@PathVariable UUID id) {
        planService.cancelPlan(getEffectiveUserId(), id);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Đã hủy kế hoạch đăng bài thành công."
        ));
    }
}
