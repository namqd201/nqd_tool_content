package com.nqd.nqd_tool_content.service.ai;

import com.nqd.nqd_tool_content.service.ai.dto.ImageRequest;
import com.nqd.nqd_tool_content.service.ai.dto.ImageResult;

public interface ImageProvider {

    String getProviderName(); // GEMINI_IMAGE, OPENAI_IMAGE, FAKE_IMAGE

    ImageResult generateImage(ImageRequest request);
}
