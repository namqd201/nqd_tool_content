package com.nqd.nqd_tool_content.repository;

import com.nqd.nqd_tool_content.entity.AiUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AiUsageLogRepository extends JpaRepository<AiUsageLog, UUID> {

    List<AiUsageLog> findByUserIdAndCreatedAtBetween(UUID userId, LocalDateTime start, LocalDateTime end);

    List<AiUsageLog> findTop50ByUserIdOrderByCreatedAtDesc(UUID userId);
}
