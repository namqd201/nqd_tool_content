package com.nqd.nqd_tool_content.service.ai.dto;

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
public class AIContentResponse {
    @Builder.Default
    private List<GeneratedPlatformPost> posts = new ArrayList<>();
    private String suggestedImagePrompt;
    @Builder.Default
    private int promptTokens = 0;
    @Builder.Default
    private int completionTokens = 0;
    @Builder.Default
    private BigDecimal estimatedCostUsd = BigDecimal.ZERO;
    private String modelUsed;
    private String providerUsed;
}
