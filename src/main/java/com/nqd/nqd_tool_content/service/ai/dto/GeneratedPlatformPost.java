package com.nqd.nqd_tool_content.service.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedPlatformPost {
    private String platform; // FACEBOOK, THREADS, X, LINKEDIN
    private String content;
    @Builder.Default
    private List<String> hashtags = new ArrayList<>();
    private List<String> threadParts;
}
