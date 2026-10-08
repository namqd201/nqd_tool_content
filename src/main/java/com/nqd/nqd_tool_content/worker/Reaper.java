package com.nqd.nqd_tool_content.worker;

import com.nqd.nqd_tool_content.entity.Post;
import com.nqd.nqd_tool_content.repository.PostRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class Reaper {

    private final PostRepository postRepository;

    /**
     * Executes immediately upon application startup before schedulers take over.
     */
    @PostConstruct
    public void onStartup() {
        log.info("[Reaper] Executing initial startup reap cycle...");
        reapExpiredLeases();
    }

    /**
     * Runs every 60 seconds to reclaim hanging leases.
     */
    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void reapExpiredLeases() {
        LocalDateTime now = LocalDateTime.now();

        // 1. GENERATING with expired lease -> back to PLANNED (re-generate)
        List<Post> expiredGenerating = postRepository
                .findByStatusAndLeaseExpiresAtLessThanEqualAndIsDeletedFalse("GENERATING", now);
        for (Post p : expiredGenerating) {
            log.warn("[Reaper] Reclaiming stuck GENERATING post {} (lease expired at {})", p.getId(), p.getLeaseExpiresAt());
            p.setStatus("PLANNED");
            p.setLockedBy(null);
            p.setLockedAt(null);
            p.setLeaseExpiresAt(null);
            postRepository.save(p);
        }

        // 2. PUBLISHING with expired lease:
        // - If request_sent_at is NULL -> Never called platform -> Return to READY
        // - If request_sent_at is NOT NULL -> Uncertain if published -> Move to RECONCILING
        List<Post> expiredPublishing = postRepository
                .findByStatusAndLeaseExpiresAtLessThanEqualAndIsDeletedFalse("PUBLISHING", now);
        for (Post p : expiredPublishing) {
            if (p.getRequestSentAt() == null) {
                log.warn("[Reaper] Reclaiming PUBLISHING post {} (request was never sent) -> READY", p.getId());
                p.setStatus("READY");
                p.setAttemptCount(Math.max(0, (p.getAttemptCount() != null ? p.getAttemptCount() : 1) - 1)); // Refund attempt
                p.setLockedBy(null);
                p.setLockedAt(null);
                p.setLeaseExpiresAt(null);
            } else {
                log.warn("[Reaper] Reclaiming PUBLISHING post {} (request already sent at {}) -> RECONCILING",
                        p.getId(), p.getRequestSentAt());
                p.setStatus("RECONCILING");
                p.setErrorClass("UNCERTAIN");
                p.setNextAttemptAt(now.plusMinutes(1));
                p.setLockedBy(null);
                p.setLockedAt(null);
                p.setLeaseExpiresAt(null);
            }
            postRepository.save(p);
        }
    }
}
