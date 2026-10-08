package com.nqd.nqd_tool_content.service;

import com.nqd.nqd_tool_content.dto.request.CreatePlanRequest;
import com.nqd.nqd_tool_content.dto.response.PlanPreviewResponse;
import com.nqd.nqd_tool_content.dto.response.PlanResponse;

import java.util.List;
import java.util.UUID;

public interface PlanService {

    List<PlanResponse> getPlans(UUID userId);

    PlanResponse getPlan(UUID userId, UUID planId);

    PlanPreviewResponse preview(CreatePlanRequest request);

    PlanResponse createDraft(UUID userId, CreatePlanRequest request);

    PlanResponse updateDraft(UUID userId, UUID planId, CreatePlanRequest request);

    PlanResponse activatePlan(UUID userId, UUID planId);

    PlanResponse pausePlan(UUID userId, UUID planId);

    PlanResponse resumePlan(UUID userId, UUID planId);

    void cancelPlan(UUID userId, UUID planId);
}
