package com.nqd.nqd_tool_content.service.storage.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.nqd.nqd_tool_content.entity.MediaAsset;
import com.nqd.nqd_tool_content.repository.MediaAssetRepository;
import com.nqd.nqd_tool_content.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class CloudinaryStorageServiceImpl implements StorageService {

    private final Cloudinary cloudinary;
    private final MediaAssetRepository mediaAssetRepository;
    private final LocalStorageServiceImpl localFallback;
    private final RestClient.Builder restClientBuilder;

    @Value("${cloudinary.cloud-name:}")
    private String cloudName;

    @Value("${CLOUDINARY_URL:}")
    private String cloudinaryUrl;

    private boolean isCloudinaryConfigured() {
        return (cloudName != null && !cloudName.isBlank()) || (cloudinaryUrl != null && !cloudinaryUrl.isBlank());
    }

    @Override
    public MediaAsset store(UUID userId, byte[] data, String mimeType, String prompt, String provider, String model) {
        if (!isCloudinaryConfigured()) {
            log.info("Cloudinary is not configured. Falling back to local/DB storage.");
            return localFallback.store(userId, data, mimeType, prompt, provider, model);
        }

        String token = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(data, ObjectUtils.asMap(
                    "folder", "nqdsm/media",
                    "public_id", token,
                    "resource_type", "image",
                    "overwrite", true
            ));

            String secureUrl = (String) uploadResult.get("secure_url");
            Long bytes = uploadResult.get("bytes") instanceof Number ? ((Number) uploadResult.get("bytes")).longValue() : (long) data.length;
            Integer width = uploadResult.get("width") instanceof Number ? ((Number) uploadResult.get("width")).intValue() : null;
            Integer height = uploadResult.get("height") instanceof Number ? ((Number) uploadResult.get("height")).intValue() : null;

            log.info("Successfully uploaded image to Cloudinary CDN: {}", secureUrl);

            MediaAsset asset = MediaAsset.builder()
                    .userId(userId)
                    .type("IMAGE")
                    .source("AI_GENERATED")
                    .storageKey(secureUrl)
                    .publicToken(token)
                    .mimeType(mimeType != null ? mimeType : "image/jpeg")
                    .sizeBytes(bytes)
                    .width(width)
                    .height(height)
                    .prompt(prompt)
                    .imageProvider(provider)
                    .model(model)
                    .data(data)
                    .build();

            return mediaAssetRepository.save(asset);
        } catch (Exception e) {
            log.error("Failed to upload image to Cloudinary, falling back to local/DB storage", e);
            return localFallback.store(userId, data, mimeType, prompt, provider, model);
        }
    }

    @Override
    public byte[] getMediaBytes(String publicToken) {
        MediaAsset asset = mediaAssetRepository.findByPublicTokenAndIsDeletedFalse(publicToken)
                .orElseThrow(() -> new IllegalArgumentException("Media not found: " + publicToken));

        // 1. Return from DB if present
        if (asset.getData() != null && asset.getData().length > 0) {
            return asset.getData();
        }

        // 2. Download from Cloudinary URL if available
        if (asset.getStorageKey() != null && asset.getStorageKey().startsWith("http")) {
            try {
                return restClientBuilder.build().get()
                        .uri(asset.getStorageKey())
                        .retrieve()
                        .body(byte[].class);
            } catch (Exception ex) {
                log.warn("Failed to download image from Cloudinary URL: {}", asset.getStorageKey(), ex);
            }
        }

        // 3. Fallback to local
        return localFallback.getMediaBytes(publicToken);
    }

    @Override
    public String getPublicUrl(String publicToken) {
        MediaAsset asset = mediaAssetRepository.findByPublicTokenAndIsDeletedFalse(publicToken).orElse(null);
        if (asset != null && asset.getStorageKey() != null && asset.getStorageKey().startsWith("http")) {
            return asset.getStorageKey();
        }
        return localFallback.getPublicUrl(publicToken);
    }
}
