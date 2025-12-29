package com.opencore.user.persistence;

import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
public class OrgMembershipId implements Serializable {
    public UUID orgId;
    public UUID userId;

    public OrgMembershipId() {}

    public OrgMembershipId(UUID orgId, UUID userId) {
        this.orgId = orgId;
        this.userId = userId;
    }
}
