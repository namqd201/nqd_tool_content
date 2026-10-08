package com.nqd.nqd_tool_content.service.ai.impl;

import com.nqd.nqd_tool_content.service.ai.ImageProvider;
import com.nqd.nqd_tool_content.service.ai.dto.ImageRequest;
import com.nqd.nqd_tool_content.service.ai.dto.ImageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAIImageProviderImpl implements ImageProvider {

    private final FakeImageProvider fakeImageProvider;

    @Override
    public String getProviderName() {
        return "OPENAI_IMAGE";
    }

    @Override
    public ImageResult generateImage(ImageRequest request) {
        ImageResult res = fakeImageProvider.generateImage(request);
        res.setProviderUsed(getProviderName());
        res.setModelUsed("dall-e-3");
        return res;
    }
}
