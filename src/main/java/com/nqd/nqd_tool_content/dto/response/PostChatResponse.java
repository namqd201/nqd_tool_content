package com.nqd.nqd_tool_content.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostChatResponse {
    private String reply;
    private String suggestedContent;
    private List<String> suggestedHashtags;
    private String modelUsed;
}
