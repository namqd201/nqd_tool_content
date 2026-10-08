package com.nqd.nqd_tool_content.repository;

import com.nqd.nqd_tool_content.entity.PostAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PostAttemptRepository extends JpaRepository<PostAttempt, UUID> {

    List<PostAttempt> findByPostIdOrderByAttemptNoAsc(UUID postId);
}
