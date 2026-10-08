package com.nqd.nqd_tool_content.service.ai;

import com.nqd.nqd_tool_content.dto.request.PostChatRequest;
import com.nqd.nqd_tool_content.dto.response.PostChatResponse;
import com.nqd.nqd_tool_content.service.ai.dto.AIContentRequest;
import com.nqd.nqd_tool_content.service.ai.dto.AIContentResponse;
import com.nqd.nqd_tool_content.service.ai.dto.PlanAnglesRequest;

import java.util.List;

public interface AIProvider {

    String getProviderName(); // GEMINI, OPENAI, ANTHROPIC, FAKE

    AIContentResponse generateContent(AIContentRequest request);

    List<String> generatePlanAngles(PlanAnglesRequest request);

    default PostChatResponse chat(PostChatRequest request, String topic, String angle, String tone, String platform) {
        return PostChatResponse.builder()
                .reply("Chào bạn! Tôi có thể giúp gì cho bạn để hoàn thiện bài viết này?")
                .suggestedContent(request.getCurrentContent())
                .modelUsed("default")
                .build();
    }
}
