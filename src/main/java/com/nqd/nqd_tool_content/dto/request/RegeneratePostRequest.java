package com.nqd.nqd_tool_content.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegeneratePostRequest {
    private String customPrompt;
    private String tone;
    private String customTone;
    private String angle;
    private UUID templateId;
    private String templateStructure;
}
