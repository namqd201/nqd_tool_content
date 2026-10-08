package com.nqd.nqd_tool_content.service.storage;

import com.nqd.nqd_tool_content.entity.MediaAsset;

import java.io.InputStream;
import java.util.UUID;

public interface StorageService {

    MediaAsset store(UUID userId, byte[] data, String mimeType, String prompt, String provider, String model);

    byte[] getMediaBytes(String publicToken);

    String getPublicUrl(String publicToken);
}
