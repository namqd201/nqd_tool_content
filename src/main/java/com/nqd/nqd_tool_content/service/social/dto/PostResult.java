package com.nqd.nqd_tool_content.service.social.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostResult {
    private String platformPostId;
    private String postUrl;
    private String status; // PUBLISHED, FAILED, NEEDS_RECONCILE
    private String errorCode;
    private String errorMessage;
    private String rawResponse;
    private boolean retryable;

    public static PostResult success(String platformPostId, String postUrl, String rawResponse) {
        return PostResult.builder()
                .status("PUBLISHED")
                .platformPostId(platformPostId)
                .postUrl(postUrl)
                .rawResponse(rawResponse)
                .retryable(false)
                .build();
    }

    public static PostResult failed(String errorCode, String errorMessage, boolean retryable, String rawResponse) {
        return PostResult.builder()
                .status("FAILED")
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .retryable(retryable)
                .rawResponse(rawResponse)
                .build();
    }

    public static PostResult needsReconcile(String errorMessage, String rawResponse) {
        return PostResult.builder()
                .status("NEEDS_RECONCILE")
                .errorMessage(errorMessage)
                .rawResponse(rawResponse)
                .retryable(false)
                .build();
    }
}
