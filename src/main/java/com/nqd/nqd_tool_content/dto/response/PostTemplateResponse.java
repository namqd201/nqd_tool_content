package com.nqd.nqd_tool_content.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostTemplateResponse {
    private UUID id;
    private String name;
    private String description;
    private String structure;
    private String exampleContent;
    private Boolean isDefault;
    private Boolean isSystem;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
