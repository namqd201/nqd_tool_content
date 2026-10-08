package com.nqd.nqd_tool_content.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiPipelineProgressResponse {

    private long totalPlanned;
    private long totalGenerating;
    private long totalReady;
    private long totalFailed;

    private List<ActivePipelineTask> activeTasks;
    private List<AiRecentActivity> recentActivities;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivePipelineTask {
        private UUID postId;
        private UUID planId;
        private String planName;
        private String platform;
        private String topic;
        private String status; // GENERATING, PLANNED, READY, GENERATION_FAILED
        private int currentStep; // 1: Khám phá chủ đề & Góc nhìn, 2: Áp dụng Tone/Brand, 3: Sinh nội dung đa kênh, 4: Kiểm duyệt an toàn (ContentGuard), 5: Tạo ảnh & Hoàn tất
        private String currentStepName;
        private String progressDescription;
        private int percentComplete;
        private String modelUsed;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiRecentActivity {
        private UUID id;
        private String kind;
        private String feature;
        private String provider;
        private String model;
        private int promptTokens;
        private int completionTokens;
        private long latencyMs;
        private String status;
        private String error;
        private LocalDateTime createdAt;
    }
}
