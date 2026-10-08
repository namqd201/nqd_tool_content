package com.nqd.nqd_tool_content.worker;

import com.nqd.nqd_tool_content.entity.Post;
import com.nqd.nqd_tool_content.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
@RequiredArgsConstructor
public class PublishingScheduler {

    private final PostRepository postRepository;
    private final PublishWorker publishWorker;

    @Value("${scheduler.publishing.batch-size:10}")
    private int batchSize;

    @Value("${scheduler.publishing.pool-size:4}")
    private int poolSize;

    private static final String SCHEDULER_WORKER_PREFIX = "pub-worker-";
    private final ExecutorService executorService = Executors.newFixedThreadPool(4);

    private volatile LocalDateTime lastTickAt = LocalDateTime.now();

    /**
     * Polls every 10 seconds for READY posts whose next_attempt_at <= now.
     * Uses atomic FOR UPDATE SKIP LOCKED query to prevent race conditions.
     */
    @Scheduled(fixedDelay = 10000)
    public void runSchedulerCycle() {
        this.lastTickAt = LocalDateTime.now();

        List<UUID> claimedPostIds = claimReadyPosts();
        if (claimedPostIds.isEmpty()) {
            return;
        }

        log.info("[PublishingScheduler] Claimed {} READY posts for execution", claimedPostIds.size());

        for (int i = 0; i < claimedPostIds.size(); i++) {
            UUID postId = claimedPostIds.get(i);
            String workerId = SCHEDULER_WORKER_PREFIX + (i % poolSize);

            executorService.submit(() -> {
                try {
                    publishWorker.processPost(postId, workerId);
                } catch (Exception e) {
                    log.error("[PublishingScheduler] Unexpected error processing post {}: {}", postId, e.getMessage(), e);
                }
            });
        }
    }

    @Transactional
    public List<UUID> claimReadyPosts() {
        LocalDateTime now = LocalDateTime.now();
        List<UUID> readyIds = postRepository.claimReadyPostIds(now, batchSize);
        if (readyIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<UUID> claimed = new ArrayList<>();
        LocalDateTime leaseExpires = now.plusSeconds(300); // 5 minutes lease

        for (UUID id : readyIds) {
            Post post = postRepository.findById(id).orElse(null);
            if (post != null && "READY".equalsIgnoreCase(post.getStatus())) {
                post.setStatus("PUBLISHING");
                post.setLockedBy("scheduler-claim");
                post.setLockedAt(now);
                post.setLeaseExpiresAt(leaseExpires);
                post.setAttemptCount((post.getAttemptCount() != null ? post.getAttemptCount() : 0) + 1);
                post.setRequestSentAt(null);
                postRepository.save(post);
                claimed.add(id);
            }
        }

        return claimed;
    }

    public LocalDateTime getLastTickAt() {
        return lastTickAt;
    }
}
