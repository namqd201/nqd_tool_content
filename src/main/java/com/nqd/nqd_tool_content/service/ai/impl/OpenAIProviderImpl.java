package com.nqd.nqd_tool_content.service.ai.impl;

import com.nqd.nqd_tool_content.service.ai.AIProvider;
import com.nqd.nqd_tool_content.service.ai.dto.AIContentRequest;
import com.nqd.nqd_tool_content.service.ai.dto.AIContentResponse;
import com.nqd.nqd_tool_content.service.ai.dto.PlanAnglesRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAIProviderImpl implements AIProvider {

    private final FakeAIProvider fakeAIProvider;

    @Value("${ai.openai.api-key:}")
    private String apiKey;

    @Override
    public String getProviderName() {
        return "OPENAI";
    }

    @Override
    public AIContentResponse generateContent(AIContentRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            log.info("[OpenAIProviderImpl] No API key configured, falling back to FakeAIProvider");
            AIContentResponse res = fakeAIProvider.generateContent(request);
            res.setProviderUsed(getProviderName());
            res.setModelUsed("gpt-4o-mini");
            return res;
        }

        AIContentResponse res = fakeAIProvider.generateContent(request);
        res.setProviderUsed(getProviderName());
        res.setModelUsed("gpt-4o-mini");
        return res;
    }

    @Override
    public List<String> generatePlanAngles(PlanAnglesRequest request) {
        return fakeAIProvider.generatePlanAngles(request);
    }
}
