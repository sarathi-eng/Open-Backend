package com.opencore.user.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnTransformer;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_logs", schema = "user_service")
public class AuditLogEntity {
    @Id
    public UUID id;

    @Column(name = "org_id")
    public UUID orgId;

    @Column(name = "actor_user_id")
    public UUID actorUserId;

    @Column(nullable = false)
    public String action;

    @Column(name = "resource_type")
    public String resourceType;

    @Column(name = "resource_id")
    public String resourceId;

    @Column
    public String ip;

    @Column(name = "metadata_json", columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    public String metadataJson;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;
}
