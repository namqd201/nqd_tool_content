package com.nqd.nqd_tool_content.repository;

import com.nqd.nqd_tool_content.entity.PostMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PostMediaRepository extends JpaRepository<PostMedia, UUID> {

    List<PostMedia> findByPostIdOrderByOrderIndexAsc(UUID postId);

    void deleteByPostId(UUID postId);
}
