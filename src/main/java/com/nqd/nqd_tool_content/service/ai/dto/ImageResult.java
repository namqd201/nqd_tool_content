package com.nqd.nqd_tool_content.service.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageResult {
    private byte[] imageBytes;
    private String mimeType; // image/jpeg, image/png, image/webp
    private Integer width;
    private Integer height;
    @Builder.Default
    private BigDecimal estimatedCostUsd = new BigDecimal("0.0200");
    private String providerUsed;
    private String modelUsed;
    private String promptUsed;
}
