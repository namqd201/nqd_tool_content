package com.nqd.nqd_tool_content.service.ai.impl;

import com.nqd.nqd_tool_content.service.ai.ImageProvider;
import com.nqd.nqd_tool_content.service.ai.dto.ImageRequest;
import com.nqd.nqd_tool_content.service.ai.dto.ImageResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Base64;

@Slf4j
@Component
public class FakeImageProvider implements ImageProvider {

    // 1x1 transparent PNG bytes in base64
    private static final String TINY_PNG_BASE64 =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";

    @Override
    public String getProviderName() {
        return "FAKE_IMAGE";
    }

    @Override
    public ImageResult generateImage(ImageRequest request) {
        log.info("[FakeImageProvider] Generating mock image for prompt: {}", request.getPrompt());
        byte[] bytes = Base64.getDecoder().decode(TINY_PNG_BASE64);

        return ImageResult.builder()
                .imageBytes(bytes)
                .mimeType("image/png")
                .width(1024)
                .height(1024)
                .estimatedCostUsd(new BigDecimal("0.0200"))
                .providerUsed(getProviderName())
                .modelUsed("mock-imagen-v1")
                .promptUsed(request.getPrompt())
                .build();
    }
}
