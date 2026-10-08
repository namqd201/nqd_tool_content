package com.nqd.nqd_tool_content.service.social;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class SocialAdapterRegistry {

    private final Map<String, SocialPlatformAdapter> adapters = new HashMap<>();

    public SocialAdapterRegistry(List<SocialPlatformAdapter> adapterList) {
        for (SocialPlatformAdapter adapter : adapterList) {
            adapters.put(adapter.getPlatformCode().toUpperCase(), adapter);
        }
    }

    public Optional<SocialPlatformAdapter> getAdapter(String platform) {
        if (platform == null) return Optional.empty();
        SocialPlatformAdapter adapter = adapters.get(platform.toUpperCase());
        if (adapter == null) {
            // fallback to FAKE if not found
            adapter = adapters.get("FAKE");
        }
        return Optional.ofNullable(adapter);
    }
}
