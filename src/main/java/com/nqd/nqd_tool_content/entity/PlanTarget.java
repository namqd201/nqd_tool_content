package com.nqd.nqd_tool_content.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "plan_targets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanTarget extends BaseIdEntity {

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Column(name = "social_account_id", nullable = false)
    private UUID socialAccountId;

    @Column(name = "posts_per_day")
    private Integer postsPerDay;
}
