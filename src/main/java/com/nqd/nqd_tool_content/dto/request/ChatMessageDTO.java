package com.nqd.nqd_tool_content.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDTO {
    private String role; // "user" or "assistant"
    private String content;
    @Builder.Default
    private java.util.List<ChatAttachmentDTO> attachments = new java.util.ArrayList<>();
}
