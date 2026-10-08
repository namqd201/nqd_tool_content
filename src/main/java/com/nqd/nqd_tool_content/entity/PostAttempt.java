package com.nqd.nqd_tool_content.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "post_attempts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostAttempt extends BaseIdEntity {

    @Column(name = "post_id", nullable = false)
    private UUID postId;

    @Builder.Default
    @Column(name = "publish_cycle", nullable = false)
    private Integer publishCycle = 1;

    @Builder.Default
    @Column(name = "attempt_no", nullable = false)
    private Integer attemptNo = 1;

    @Column(name = "worker_id", length = 100)
    private String workerId;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "request_sent_at")
    private LocalDateTime requestSentAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "outcome", nullable = false, length = 50)
    private String outcome;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "platform_error_code", length = 100)
    private String platformErrorCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "response_snippet", columnDefinition = "TEXT")
    private String responseSnippet;
}
