package com.nqd.nqd_tool_content.service.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageRequest {
    private String prompt;
    private String style; // minimalist, cinematic, photorealistic, digital_art
    private String aspectRatio; // 1:1, 16:9, 4:5
}
