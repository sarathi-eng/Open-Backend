package com.opencore.user.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "org_memberships", schema = "user_service")
public class OrgMembershipEntity {
    @EmbeddedId
    public OrgMembershipId id;

    @Column(nullable = false)
    public String role;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;
}
