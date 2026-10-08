package com.nqd.nqd_tool_content.controller;

import com.nqd.nqd_tool_content.entity.MediaAsset;
import com.nqd.nqd_tool_content.repository.MediaAssetRepository;
import com.nqd.nqd_tool_content.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/media")
@RequiredArgsConstructor
public class MediaController {

    private final StorageService storageService;
    private final MediaAssetRepository mediaAssetRepository;

    @GetMapping("/{token}")
    public ResponseEntity<byte[]> getMedia(@PathVariable String token) {
        MediaAsset asset = mediaAssetRepository.findByPublicTokenAndIsDeletedFalse(token).orElse(null);
        if (asset == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        try {
            byte[] bytes = storageService.getMediaBytes(token);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(asset.getMimeType() != null ? asset.getMimeType() : "image/jpeg"));
            headers.setContentLength(bytes.length);
            headers.setCacheControl("public, max-age=86400"); // Cache 1 ngày
            return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
