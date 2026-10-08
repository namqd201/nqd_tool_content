package com.nqd.nqd_tool_content.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "media_assets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaAsset extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Builder.Default
    @Column(name = "type", nullable = false, length = 30)
    private String type = "IMAGE"; // IMAGE, VIDEO

    @Builder.Default
    @Column(name = "source", nullable = false, length = 30)
    private String source = "AI_GENERATED"; // AI_GENERATED, UPLOADED

    @Column(name = "storage_key", length = 500)
    private String storageKey;

    @Column(name = "public_token", nullable = false, unique = true, length = 64)
    private String publicToken;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "sha256", length = 64)
    private String sha256;

    @Column(name = "prompt", columnDefinition = "TEXT")
    private String prompt;

    @Column(name = "image_provider", length = 50)
    private String imageProvider;

    @Column(name = "model", length = 50)
    private String model;

    @Basic(fetch = FetchType.LAZY)
    @Column(name = "data", columnDefinition = "bytea")
    private byte[] data;
}
