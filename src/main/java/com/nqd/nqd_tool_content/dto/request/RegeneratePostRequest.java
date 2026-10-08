package com.nqd.nqd_tool_content.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegeneratePostRequest {
    private String customPrompt;
    private String tone;
    private String angle;
}
