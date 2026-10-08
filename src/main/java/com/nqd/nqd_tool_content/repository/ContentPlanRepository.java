package com.nqd.nqd_tool_content.repository;

import com.nqd.nqd_tool_content.entity.ContentPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContentPlanRepository extends JpaRepository<ContentPlan, UUID> {

    List<ContentPlan> findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID userId);

    long countByUserIdAndStatusAndIsDeletedFalse(UUID userId, String status);
}
