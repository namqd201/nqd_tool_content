package com.nqd.nqd_tool_content.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostResponse {
    private String id;
    private String planId;
    private String planName;
    private String source;
    private String groupId;
    private String slotKey;
    private String socialAccountId;
    private String channelName;
    private String platform;
    private LocalDateTime scheduledAt;
    private String scheduledTimezone;
    private LocalDateTime generateAt;
    private String status;
    private String content;
    private String hashtags;
    private String threadParts;
    private String imagePrompt;
    private Boolean contentEditedByUser;
    private String angle;
    private Integer publishCycle;
    private Integer attemptCount;
    private Integer maxAttempts;
    private LocalDateTime nextAttemptAt;
    private String errorClass;
    private String lastError;
    private String needsReviewReason;
    private String platformPostId;
    private String platformPostUrl;
    private LocalDateTime publishedAt;
    private List<String> mediaUrls;
}
