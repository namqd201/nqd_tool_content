package com.nqd.nqd_tool_content.repository;

import com.nqd.nqd_tool_content.entity.UserIdentity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserIdentityRepository extends JpaRepository<UserIdentity, UUID> {

    Optional<UserIdentity> findByProviderAndProviderUserId(String provider, String providerUserId);

    List<UserIdentity> findByUserId(UUID userId);

    Optional<UserIdentity> findByProviderAndEmail(String provider, String email);
}
