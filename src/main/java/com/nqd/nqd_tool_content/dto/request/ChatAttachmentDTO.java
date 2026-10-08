package com.nqd.nqd_tool_content.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatAttachmentDTO {
    private String fileName;
    private String mimeType;
    private String base64Data;
}
