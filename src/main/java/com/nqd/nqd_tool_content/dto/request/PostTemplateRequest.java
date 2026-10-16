package com.nqd.nqd_tool_content.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostTemplateRequest {

    @NotBlank(message = "Tên form mẫu không được để trống")
    private String name;

    private String description;

    @NotBlank(message = "Cấu trúc form mẫu không được để trống")
    private String structure;

    private String exampleContent;

    private Boolean isDefault;
}
