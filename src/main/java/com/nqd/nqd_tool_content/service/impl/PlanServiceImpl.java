package com.nqd.nqd_tool_content.service.impl;

import com.nqd.nqd_tool_content.dto.request.CreatePlanRequest;
import com.nqd.nqd_tool_content.dto.response.PlanPreviewResponse;
import com.nqd.nqd_tool_content.dto.response.PlanResponse;
import com.nqd.nqd_tool_content.entity.*;
import com.nqd.nqd_tool_content.exception.ResourceNotFoundException;
import com.nqd.nqd_tool_content.repository.*;
import com.nqd.nqd_tool_content.service.AuditService;
import com.nqd.nqd_tool_content.service.PlanService;
import com.nqd.nqd_tool_content.service.plan.CalculatedSlot;
import com.nqd.nqd_tool_content.service.plan.SlotCalculator;
import com.nqd.nqd_tool_content.worker.ContentGenerationWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlanServiceImpl implements PlanService {

    private final ContentPlanRepository planRepository;
    private final PlanTargetRepository targetRepository;
    private final SocialAccountRepository accountRepository;
    private final PostRepository postRepository;
    private final SlotCalculator slotCalculator;
    private final AuditService auditService;
    private final ContentGenerationWorker contentGenerationWorker;

    @Override
    @Transactional(readOnly = true)
    public List<PlanResponse> getPlans(UUID userId) {
        return planRepository.findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId).stream()
                .map(this::toPlanResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PlanResponse getPlan(UUID userId, UUID planId) {
        ContentPlan plan = findUserPlan(userId, planId);
        return toPlanResponse(plan);
    }

    @Override
    public PlanPreviewResponse preview(CreatePlanRequest request) {
        String tz = (request.getTimezone() != null && !request.getTimezone().isBlank())
                ? request.getTimezone()
                : "Asia/Ho_Chi_Minh";
        int postsPerDay = request.getPostsPerDay() != null ? request.getPostsPerDay() : 1;
        boolean includeImg = Boolean.TRUE.equals(request.getIncludeImage());

        List<CalculatedSlot> slots = slotCalculator.calculateSlots(
                request.getStartDate(),
                request.getEndDate(),
                tz,
                postsPerDay,
                request.getTimeMode(),
                request.getTimeSlots(),
                12
        );

        int totalDays = (int) ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;
        int totalSlotsPerChannel = slots.size();
        int channelCount = (request.getTargetAccountIds() != null && !request.getTargetAccountIds().isEmpty())
                ? request.getTargetAccountIds().size()
                : 1;
        int totalPosts = totalSlotsPerChannel * channelCount;

        // Ước tính chi phí AI: ~$0.005 văn bản + $0.02 ảnh nếu có
        BigDecimal costPerPost = includeImg ? new BigDecimal("0.025") : new BigDecimal("0.005");
        BigDecimal estimatedCost = costPerPost.multiply(new BigDecimal(totalPosts));

        return PlanPreviewResponse.builder()
                .totalDays(totalDays)
                .postsPerDay(request.getPostsPerDay())
                .totalSlotsPerChannel(totalSlotsPerChannel)
                .totalPosts(totalPosts)
                .estimatedCostUsd(estimatedCost)
                .slots(slots)
                .build();
    }

    @Override
    @Transactional
    public PlanResponse createDraft(UUID userId, CreatePlanRequest request) {
        validatePlanRequest(request);

        ContentPlan plan = ContentPlan.builder()
                .userId(userId)
                .name(request.getName())
                .topic(request.getTopic())
                .instructions(request.getInstructions())
                .language(request.getLanguage())
                .tone(request.getTone())
                .status("DRAFT")
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .timezone(request.getTimezone() != null && !request.getTimezone().isBlank() ? request.getTimezone() : "Asia/Ho_Chi_Minh")
                .postsPerDay(request.getPostsPerDay())
                .timeMode(request.getTimeMode() != null ? request.getTimeMode() : "FIXED_TIMES")
                .timeSlots(toJson(request.getTimeSlots()))

                .includeImage(request.getIncludeImage())
                .imageStyle(request.getImageStyle())
                .imageFailurePolicy(request.getImageFailurePolicy())
                .contentMode(request.getContentMode())
                .estimatedCostUsd(preview(request).getEstimatedCostUsd())
                .build();

        plan = planRepository.save(plan);

        saveTargets(plan.getId(), request.getTargetAccountIds());

        auditService.log(userId, "CREATE_PLAN_DRAFT", "ContentPlan", plan.getId().toString(),
                "Tạo bản nháp kế hoạch: " + plan.getName());

        return toPlanResponse(plan);
    }

    @Override
    @Transactional
    public PlanResponse updateDraft(UUID userId, UUID planId, CreatePlanRequest request) {
        ContentPlan plan = findUserPlan(userId, planId);
        if (!"DRAFT".equals(plan.getStatus()) && !"PAUSED".equals(plan.getStatus())) {
            throw new IllegalStateException("Chỉ có thể chỉnh sửa kế hoạch ở trạng thái Bản nháp hoặc Tạm dừng");
        }

        validatePlanRequest(request);

        plan.setName(request.getName());
        plan.setTopic(request.getTopic());
        plan.setInstructions(request.getInstructions());
        plan.setLanguage(request.getLanguage());
        plan.setTone(request.getTone());
        plan.setStartDate(request.getStartDate());
        plan.setEndDate(request.getEndDate());
        plan.setTimezone(request.getTimezone());
        plan.setPostsPerDay(request.getPostsPerDay());
        plan.setTimeMode(request.getTimeMode());
        plan.setTimeSlots(toJson(request.getTimeSlots()));
        plan.setIncludeImage(request.getIncludeImage());
        plan.setImageStyle(request.getImageStyle());
        plan.setImageFailurePolicy(request.getImageFailurePolicy());
        plan.setContentMode(request.getContentMode());
        plan.setEstimatedCostUsd(preview(request).getEstimatedCostUsd());

        plan = planRepository.save(plan);

        targetRepository.deleteByPlanId(plan.getId());
        saveTargets(plan.getId(), request.getTargetAccountIds());

        auditService.log(userId, "UPDATE_PLAN", "ContentPlan", plan.getId().toString(),
                "Cập nhật kế hoạch: " + plan.getName());

        return toPlanResponse(plan);
    }

    @Override
    @Transactional
    public PlanResponse activatePlan(UUID userId, UUID planId) {
        ContentPlan plan = findUserPlan(userId, planId);

        List<PlanTarget> targets = targetRepository.findByPlanId(plan.getId());
        if (targets.isEmpty()) {
            throw new IllegalStateException("Kế hoạch chưa chọn kênh đăng bài nào");
        }

        // Kiểm tra xem các kênh có active không
        List<SocialAccount> accounts = accountRepository.findAllById(
                targets.stream().map(PlanTarget::getSocialAccountId).collect(Collectors.toList())
        );
        boolean hasActiveChannel = accounts.stream().anyMatch(a -> a.getIsEnabled() && "ACTIVE".equals(a.getStatus()));
        if (!hasActiveChannel) {
            throw new IllegalStateException("Không có kênh nào đang bật và có trạng thái hoạt động (ACTIVE)");
        }

        // Sinh slots
        List<String> rawSlots = fromJsonList(plan.getTimeSlots());
        List<CalculatedSlot> calculatedSlots = slotCalculator.calculateSlots(
                plan.getStartDate(),
                plan.getEndDate(),
                plan.getTimezone(),
                plan.getPostsPerDay(),
                plan.getTimeMode(),
                rawSlots,
                plan.getGenerateLeadHours()
        );

        LocalDateTime now = LocalDateTime.now();

        // Tạo các Post cho từng slot và kênh
        for (CalculatedSlot cs : calculatedSlots) {
            UUID groupId = UUID.randomUUID();

            for (SocialAccount acc : accounts) {
                if (!acc.getIsEnabled() || !"ACTIVE".equals(acc.getStatus())) {
                    continue;
                }

                // Nếu slot đã qua trong quá khứ thì đánh dấu SKIPPED
                String initialStatus = cs.getScheduledAt().isBefore(now) ? "SKIPPED" : "PLANNED";
                // Đặt generateAt = now để AI sinh nội dung ngay lập tức sau khi kích hoạt
                LocalDateTime postGenerateAt = now;

                Post post = Post.builder()
                        .userId(userId)
                        .planId(plan.getId())
                        .source("PLAN")
                        .groupId(groupId)
                        .slotKey(cs.getSlotKey())
                        .socialAccountId(acc.getId())
                        .platform(acc.getPlatform())
                        .scheduledAt(cs.getScheduledAt())
                        .scheduledTimezone(plan.getTimezone())
                        .generateAt(postGenerateAt)
                        .missedGraceMinutes(plan.getMissedGraceMinutes())
                        .status(initialStatus)
                        .nextAttemptAt(cs.getScheduledAt())
                        .publishCycle(1)
                        .attemptCount(0)
                        .maxAttempts(3)
                        .reconcileCount(0)
                        .errorClass("NONE")
                        .contentEditedByUser(false)
                        .build();

                postRepository.save(post);
            }
        }

        plan.setStatus("ACTIVE");
        plan.setActivatedAt(LocalDateTime.now());
        plan = planRepository.save(plan);

        auditService.log(userId, "ACTIVATE_PLAN", "ContentPlan", plan.getId().toString(),
                "Kích hoạt kế hoạch tự động: " + plan.getName() + " (" + calculatedSlots.size() + " slots)");

        // Kích hoạt ngay chu kỳ sinh bài AI trong nền
        new Thread(() -> {
            try {
                Thread.sleep(500); // Cho commit transaction hiện tại
                contentGenerationWorker.runGenerationCycle();
            } catch (Exception e) {
                log.error("Lỗi khi tự động kích hoạt sinh nội dung: {}", e.getMessage());
            }
        }).start();

        return toPlanResponse(plan);
    }

    @Override
    @Transactional
    public PlanResponse pausePlan(UUID userId, UUID planId) {
        ContentPlan plan = findUserPlan(userId, planId);
        plan.setStatus("PAUSED");
        plan = planRepository.save(plan);

        auditService.log(userId, "PAUSE_PLAN", "ContentPlan", plan.getId().toString(),
                "Tạm dừng kế hoạch: " + plan.getName());

        return toPlanResponse(plan);
    }

    @Override
    @Transactional
    public PlanResponse resumePlan(UUID userId, UUID planId) {
        ContentPlan plan = findUserPlan(userId, planId);
        plan.setStatus("ACTIVE");
        plan = planRepository.save(plan);

        auditService.log(userId, "RESUME_PLAN", "ContentPlan", plan.getId().toString(),
                "Tiếp tục chạy kế hoạch: " + plan.getName());

        return toPlanResponse(plan);
    }

    @Override
    @Transactional
    public void cancelPlan(UUID userId, UUID planId) {
        ContentPlan plan = findUserPlan(userId, planId);
        plan.setStatus("CANCELLED");
        planRepository.save(plan);

        // Hủy các bài chưa chạy
        List<Post> posts = postRepository.findByPlanIdAndIsDeletedFalseOrderByScheduledAtAsc(plan.getId());
        for (Post p : posts) {
            if ("PLANNED".equals(p.getStatus()) || "READY".equals(p.getStatus())) {
                p.setStatus("SKIPPED");
                p.setLastError("Kế hoạch đã bị hủy");
                postRepository.save(p);
            }
        }

        auditService.log(userId, "CANCEL_PLAN", "ContentPlan", plan.getId().toString(),
                "Hủy kế hoạch: " + plan.getName());
    }

    private ContentPlan findUserPlan(UUID userId, UUID planId) {
        return planRepository.findById(planId)
                .filter(p -> p.getUserId().equals(userId) && !Boolean.TRUE.equals(p.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Kế hoạch không tồn tại"));
    }

    private void validatePlanRequest(CreatePlanRequest req) {
        if (req.getStartDate().isAfter(req.getEndDate())) {
            throw new IllegalArgumentException("Ngày bắt đầu không được sau ngày kết thúc");
        }
        long days = ChronoUnit.DAYS.between(req.getStartDate(), req.getEndDate()) + 1;
        if (days > 60) {
            throw new IllegalArgumentException("Kế hoạch không được vượt quá 60 ngày");
        }
    }

    private void saveTargets(UUID planId, List<UUID> accountIds) {
        if (accountIds != null) {
            for (UUID accId : accountIds) {
                targetRepository.save(PlanTarget.builder()
                        .planId(planId)
                        .socialAccountId(accId)
                        .build());
            }
        }
    }

    private PlanResponse toPlanResponse(ContentPlan plan) {
        List<PlanTarget> targets = targetRepository.findByPlanId(plan.getId());
        List<String> targetIds = targets.stream()
                .map(t -> t.getSocialAccountId().toString())
                .collect(Collectors.toList());

        long totalSlots = postRepository.countByPlanIdAndIsDeletedFalse(plan.getId());
        long publishedSlots = postRepository.countByPlanIdAndStatusAndIsDeletedFalse(plan.getId(), "PUBLISHED");
        long failedSlots = postRepository.countByPlanIdAndStatusAndIsDeletedFalse(plan.getId(), "FAILED");

        return PlanResponse.builder()
                .id(plan.getId().toString())
                .name(plan.getName())
                .topic(plan.getTopic())
                .instructions(plan.getInstructions())
                .language(plan.getLanguage())
                .tone(plan.getTone())
                .status(plan.getStatus())
                .startDate(plan.getStartDate())
                .endDate(plan.getEndDate())
                .timezone(plan.getTimezone())
                .postsPerDay(plan.getPostsPerDay())
                .timeMode(plan.getTimeMode())
                .timeSlots(fromJsonList(plan.getTimeSlots()))
                .targetAccountIds(targetIds)
                .includeImage(plan.getIncludeImage())
                .imageStyle(plan.getImageStyle())
                .estimatedCostUsd(plan.getEstimatedCostUsd())
                .totalSlots(totalSlots)
                .publishedSlots(publishedSlots)
                .failedSlots(failedSlots)
                .createdAt(plan.getCreatedAt())
                .activatedAt(plan.getActivatedAt())
                .build();
    }

    private String toJson(List<String> list) {
        if (list == null || list.isEmpty()) return "";
        return String.join(";", list);
    }

    private List<String> fromJsonList(String str) {
        if (str == null || str.isBlank()) return Collections.emptyList();
        return Arrays.stream(str.split(";"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
