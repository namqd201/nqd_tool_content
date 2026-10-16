package com.nqd.nqd_tool_content.repository;

import com.nqd.nqd_tool_content.entity.PostTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PostTemplateRepository extends JpaRepository<PostTemplate, UUID> {

    List<PostTemplate> findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID userId);

    List<PostTemplate> findByIsSystemTrueAndIsDeletedFalseOrderByCreatedAtAsc();

    Optional<PostTemplate> findByIdAndUserIdAndIsDeletedFalse(UUID id, UUID userId);

    Optional<PostTemplate> findByUserIdAndIsDefaultTrueAndIsDeletedFalse(UUID userId);

    @Query("SELECT t FROM PostTemplate t WHERE (t.userId = :userId OR t.isSystem = true) AND t.isDeleted = false ORDER BY t.isDefault DESC, t.isSystem DESC, t.createdAt DESC")
    List<PostTemplate> findAllAvailableForUser(@Param("userId") UUID userId);
}
