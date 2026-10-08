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
                .data(data)
                .build();

        return mediaAssetRepository.save(asset);
    }

    @Override
    public byte[] getMediaBytes(String publicToken) {
        MediaAsset asset = mediaAssetRepository.findByPublicTokenAndIsDeletedFalse(publicToken)
                .orElseThrow(() -> new IllegalArgumentException("Media not found: " + publicToken));

        Path filePath = Paths.get(storageDir).resolve(asset.getStorageKey());
        if (Files.exists(filePath)) {
            try {
                return Files.readAllBytes(filePath);
            } catch (IOException e) {
                log.warn("Could not read media from file, falling back to database: {}", asset.getStorageKey());
            }
        }

        // Fallback: nếu disk file không tồn tại (do Render ephemeral disk hoặc restart), lấy từ DB
        if (asset.getData() != null && asset.getData().length > 0) {
            try {
                Path dirPath = Paths.get(storageDir);
                if (!Files.exists(dirPath)) {
                    Files.createDirectories(dirPath);
                }
                Files.write(filePath, asset.getData());
            } catch (Exception ex) {
                log.warn("Could not re-cache media file to disk: {}", ex.getMessage());
            }
            return asset.getData();
        }

        throw new RuntimeException("Storage read error: image not available on disk or db for " + publicToken);
    }

    @Override
    public String getPublicUrl(String publicToken) {
        return backendUrl + "/media/" + publicToken;
    }
}
