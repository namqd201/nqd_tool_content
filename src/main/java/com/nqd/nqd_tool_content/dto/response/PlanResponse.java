package com.nqd.nqd_tool_content.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanResponse {
    private String id;
    private String name;
    private String topic;
    private String instructions;
    private String language;
    private String tone;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private String timezone;
    private Integer postsPerDay;
    private String timeMode;
    @Builder.Default
    private List<String> timeSlots = new ArrayList<>();
    @Builder.Default
    private List<String> targetAccountIds = new ArrayList<>();
    private Boolean includeImage;
    private String imageStyle;
    private BigDecimal estimatedCostUsd;
    private long totalSlots;
    private long publishedSlots;
    private long failedSlots;
    private LocalDateTime createdAt;
    private LocalDateTime activatedAt;
}
