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
public class MarkPublishedRequest {
    @NotBlank(message = "URL bài đăng không được để trống")
    private String url;
}
