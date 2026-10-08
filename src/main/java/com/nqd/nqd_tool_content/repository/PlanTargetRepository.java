package com.nqd.nqd_tool_content.repository;

import com.nqd.nqd_tool_content.entity.PlanTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PlanTargetRepository extends JpaRepository<PlanTarget, UUID> {

    List<PlanTarget> findByPlanId(UUID planId);

    void deleteByPlanId(UUID planId);
}
