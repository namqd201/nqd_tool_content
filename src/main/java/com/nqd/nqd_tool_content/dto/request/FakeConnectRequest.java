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
public class FakeConnectRequest {

    @NotBlank(message = "Platform không được để trống")
    private String platform; // FACEBOOK, THREADS, X, LINKEDIN

    private String displayName;
}
