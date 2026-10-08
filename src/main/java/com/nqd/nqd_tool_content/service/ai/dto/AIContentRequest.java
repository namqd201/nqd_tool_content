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
public class AIContentRequest {
    private String topic;
    private String brief;
    private String instructions;
    private String tone;
    private String language;
    private String angle;
    @Builder.Default
    private List<String> platforms = new ArrayList<>();
    @Builder.Default
    private List<String> recentPosts = new ArrayList<>();
    @Builder.Default
    private List<String> forbiddenWords = new ArrayList<>();
    @Builder.Default
    private List<String> preferredHashtags = new ArrayList<>();
    private boolean generateImagePrompt;
    private String customPrompt;
}
