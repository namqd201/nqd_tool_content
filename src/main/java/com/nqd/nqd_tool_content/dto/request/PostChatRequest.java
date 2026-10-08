package com.nqd.nqd_tool_content.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostChatRequest {
    @NotBlank(message = "Tin nhắn không được để trống")
    private String message;

    private String currentContent;

    @Builder.Default
    private List<ChatMessageDTO> history = new ArrayList<>();

    @Builder.Default
    private List<ChatAttachmentDTO> attachments = new ArrayList<>();
}
