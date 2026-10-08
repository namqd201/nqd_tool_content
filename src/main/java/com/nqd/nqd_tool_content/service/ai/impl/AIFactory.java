package com.nqd.nqd_tool_content.service.ai.impl;

import com.nqd.nqd_tool_content.entity.UserSettings;
import com.nqd.nqd_tool_content.service.ai.AIProvider;
import com.nqd.nqd_tool_content.service.ai.ImageProvider;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AIFactory {

    private final Map<String, AIProvider> textProviders = new HashMap<>();
    private final Map<String, ImageProvider> imageProviders = new HashMap<>();

    public AIFactory(List<AIProvider> textList, List<ImageProvider> imageList) {
        for (AIProvider p : textList) {
            textProviders.put(p.getProviderName().toUpperCase(), p);
        }
        for (ImageProvider ip : imageList) {
            imageProviders.put(ip.getProviderName().toUpperCase(), ip);
        }
    }

    public AIProvider getTextProvider(String name) {
        if (name == null) return textProviders.getOrDefault("GEMINI", textProviders.get("FAKE"));
        AIProvider provider = textProviders.get(name.toUpperCase());
        return provider != null ? provider : textProviders.getOrDefault("GEMINI", textProviders.get("FAKE"));
    }

    public ImageProvider getImageProvider(String name) {
        if (name == null) return imageProviders.getOrDefault("GEMINI_IMAGE", imageProviders.get("FAKE_IMAGE"));
        ImageProvider provider = imageProviders.get(name.toUpperCase());
        return provider != null ? provider : imageProviders.getOrDefault("GEMINI_IMAGE", imageProviders.get("FAKE_IMAGE"));
    }

    public AIProvider getTextProvider(UserSettings settings) {
        String primary = settings != null && settings.getTextProviderPrimary() != null
                ? settings.getTextProviderPrimary() : "GEMINI";
        return getTextProvider(primary);
    }

    public ImageProvider getImageProvider(UserSettings settings) {
        String primary = settings != null && settings.getImageProviderPrimary() != null
                ? settings.getImageProviderPrimary() : "GEMINI_IMAGE";
        return getImageProvider(primary);
    }
}
