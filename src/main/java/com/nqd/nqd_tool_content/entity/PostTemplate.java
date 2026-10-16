package com.nqd.nqd_tool_content.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "post_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostTemplate extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "name", columnDefinition = "TEXT", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "structure", columnDefinition = "TEXT", nullable = false)
    private String structure;

    @Column(name = "example_content", columnDefinition = "TEXT")
    private String exampleContent;

    @Builder.Default
    @Column(name = "is_default")
    private Boolean isDefault = false;

    @Builder.Default
    @Column(name = "is_system")
    private Boolean isSystem = false;

    @jakarta.persistence.PrePersist
    @Override
    protected void onCreate() {
        super.onCreate();
        if (isDefault == null) isDefault = false;
        if (isSystem == null) isSystem = false;
    }
}
