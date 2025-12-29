package com.opencore.user.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "organizations", schema = "user_service")
public class OrganizationEntity {
    @Id
    public UUID id;

    @Column(nullable = false)
    public String name;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;
}
