package com.nqd.nqd_tool_content.dto.response;

import com.nqd.nqd_tool_content.service.plan.CalculatedSlot;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanPreviewResponse {
    private int totalDays;
    private int postsPerDay;
    private int totalSlotsPerChannel;
    private int totalPosts;
    private BigDecimal estimatedCostUsd;
    @Builder.Default
    private List<CalculatedSlot> slots = new ArrayList<>();
}
