package com.nqd.nqd_tool_content.service.storage.impl;

import com.nqd.nqd_tool_content.entity.MediaAsset;
import com.nqd.nqd_tool_content.repository.MediaAssetRepository;
import com.nqd.nqd_tool_content.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocalStorageServiceImpl implements StorageService {

    private final MediaAssetRepository mediaAssetRepository;

    @Value("${app.storage.dir:uploads/media}")
    private String storageDir;

    @Value("${app.backend.url:http://localhost:8080}")
    private String backendUrl;

    @Override
    public MediaAsset store(UUID userId, byte[] data, String mimeType, String prompt, String provider, String model) {
        String token = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        String filename = token + (mimeType != null && mimeType.contains("png") ? ".png" : ".jpg");

        Path dirPath = Paths.get(storageDir);
        try {
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }
            Path filePath = dirPath.resolve(filename);
            Files.write(filePath, data);
        } catch (IOException e) {
            log.error("Failed to write media file: {}", filename, e);
            throw new RuntimeException("Storage write error", e);
        }

        MediaAsset asset = MediaAsset.builder()
                .userId(userId)
                .type("IMAGE")
                .source("AI_GENERATED")
                .storageKey(filename)
                .publicToken(token)
                .mimeType(mimeType != null ? mimeType : "image/jpeg")
                .sizeBytes((long) data.length)
                .prompt(prompt)
                .imageProvider(provider)
                .model(model)
                .build();

        return mediaAssetRepository.save(asset);
    }

    @Override
    public byte[] getMediaBytes(String publicToken) {
        MediaAsset asset = mediaAssetRepository.findByPublicTokenAndIsDeletedFalse(publicToken)
                .orElseThrow(() -> new IllegalArgumentException("Media not found: " + publicToken));

        Path filePath = Paths.get(storageDir).resolve(asset.getStorageKey());
        try {
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            log.error("Failed to read media file: {}", asset.getStorageKey(), e);
            throw new RuntimeException("Storage read error", e);
        }
    }

    @Override
    public String getPublicUrl(String publicToken) {
        return backendUrl + "/media/" + publicToken;
    }
}
