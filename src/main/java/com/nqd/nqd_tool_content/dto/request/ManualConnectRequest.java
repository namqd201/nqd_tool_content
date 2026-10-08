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
public class ManualConnectRequest {

    @NotBlank(message = "Platform không được để trống")
    private String platform; // FACEBOOK, THREADS, X, LINKEDIN

    private String displayName; // Tên hiển thị kết nối

    private String accountType; // PAGE, PROFILE

    private String platformAccountId; // ID của Page / Account nếu có sẵn

    private String accessToken; // Token truy cập (VD: Page Access Token hoặc User Token)

    private String username;

    private String avatarUrl;

    private String appId; // Facebook App ID (tùy chọn để đổi sang Token dài hạn/vĩnh viễn)

    private String appSecret; // Facebook App Secret (tùy chọn để đổi sang Token dài hạn/vĩnh viễn)
}
