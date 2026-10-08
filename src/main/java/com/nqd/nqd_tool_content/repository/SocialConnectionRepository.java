package com.nqd.nqd_tool_content.repository;

import com.nqd.nqd_tool_content.entity.SocialConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SocialConnectionRepository extends JpaRepository<SocialConnection, UUID> {

    List<SocialConnection> findByUserIdAndIsDeletedFalse(UUID userId);

    Optional<SocialConnection> findByUserIdAndPlatformAndPlatformUserIdAndIsDeletedFalse(
            UUID userId, String platform, String platformUserId);

    List<SocialConnection> findByStatusAndTokenExpiresAtLessThanEqualAndIsDeletedFalse(
            String status, LocalDateTime threshold);
}
