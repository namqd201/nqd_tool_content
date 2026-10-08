package com.nqd.nqd_tool_content.repository;

import com.nqd.nqd_tool_content.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface PostRepository extends JpaRepository<Post, UUID> {

    List<Post> findByUserIdAndIsDeletedFalseOrderByScheduledAtAsc(UUID userId);

    List<Post> findByPlanIdAndIsDeletedFalseOrderByScheduledAtAsc(UUID planId);

    List<Post> findByUserIdAndScheduledAtBetweenAndIsDeletedFalse(
            UUID userId, LocalDateTime start, LocalDateTime end);

    long countByPlanIdAndStatusAndIsDeletedFalse(UUID planId, String status);

    long countByPlanIdAndIsDeletedFalse(UUID planId);

    List<Post> findByStatusAndGenerateAtLessThanEqualAndIsDeletedFalseOrderByGenerateAtAsc(
            String status, LocalDateTime generateAt);

    List<Post> findByGroupIdAndIsDeletedFalse(UUID groupId);

    @org.springframework.data.jpa.repository.Query(value = """
        SELECT id FROM posts
        WHERE status = 'READY'
          AND next_attempt_at <= :now
          AND is_deleted = false
        ORDER BY next_attempt_at ASC
        LIMIT :limit
        FOR UPDATE SKIP LOCKED
    """, nativeQuery = true)
    List<UUID> claimReadyPostIds(
            @org.springframework.data.repository.query.Param("now") LocalDateTime now,
            @org.springframework.data.repository.query.Param("limit") int limit
    );

    List<Post> findByStatusAndLeaseExpiresAtLessThanEqualAndIsDeletedFalse(
            String status, LocalDateTime now);

    List<Post> findByStatusAndNextAttemptAtLessThanEqualAndIsDeletedFalse(
            String status, LocalDateTime now);

    List<Post> findTop20ByUserIdAndStatusInOrderByUpdatedAtDesc(UUID userId, List<String> statuses);

    List<Post> findByUserIdAndStatusAndIsDeletedFalse(UUID userId, String status);

    long countByUserIdAndStatusAndIsDeletedFalse(UUID userId, String status);
}

