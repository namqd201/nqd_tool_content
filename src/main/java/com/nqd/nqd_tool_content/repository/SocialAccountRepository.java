package com.nqd.nqd_tool_content.repository;

import com.nqd.nqd_tool_content.entity.SocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SocialAccountRepository extends JpaRepository<SocialAccount, UUID> {

    List<SocialAccount> findByUserIdAndIsDeletedFalse(UUID userId);

    List<SocialAccount> findByConnectionIdAndIsDeletedFalse(UUID connectionId);

    List<SocialAccount> findByUserIdAndIsEnabledTrueAndIsDeletedFalse(UUID userId);

    List<SocialAccount> findByUserIdAndPlatformAndIsEnabledTrueAndIsDeletedFalse(UUID userId, String platform);

    Optional<SocialAccount> findByUserIdAndPlatformAndPlatformAccountIdAndIsDeletedFalse(
            UUID userId, String platform, String platformAccountId);
}
