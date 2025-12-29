package com.opencore.user.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrgMembershipRepository extends JpaRepository<OrgMembershipEntity, OrgMembershipId> {
    Optional<OrgMembershipEntity> findByIdOrgIdAndIdUserId(UUID orgId, UUID userId);

    List<OrgMembershipEntity> findAllByIdOrgId(UUID orgId);

    List<OrgMembershipEntity> findAllByIdUserId(UUID userId);
}
