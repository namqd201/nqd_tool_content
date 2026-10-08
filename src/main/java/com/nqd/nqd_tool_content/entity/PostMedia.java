package com.nqd.nqd_tool_content.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "post_media")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostMedia extends BaseIdEntity {

    @Column(name = "post_id", nullable = false)
    private UUID postId;

    @Column(name = "media_asset_id", nullable = false)
    private UUID mediaAssetId;

    @Builder.Default
    @Column(name = "order_index")
    private Integer orderIndex = 0;

    @Column(name = "alt_text", columnDefinition = "TEXT")
    private String altText;
}
