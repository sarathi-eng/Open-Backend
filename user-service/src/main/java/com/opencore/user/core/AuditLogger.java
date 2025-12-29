package com.opencore.user.core;

import com.opencore.user.persistence.AuditLogEntity;
import com.opencore.user.persistence.AuditLogRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class AuditLogger {
    private final AuditLogRepository audit;

    public AuditLogger(AuditLogRepository audit) {
        this.audit = audit;
    }

    public void log(String action, String resourceType, String resourceId, String metadataJson) {
        RequestContext ctx = RequestContextHolder.get();

        AuditLogEntity e = new AuditLogEntity();
        e.id = UUID.randomUUID();
        e.orgId = ctx == null ? null : ctx.orgId();
        e.actorUserId = ctx == null ? null : ctx.actorUserId();
        e.action = action;
        e.resourceType = resourceType;
        e.resourceId = resourceId;
        e.ip = ctx == null ? null : ctx.ip();
        e.metadataJson = metadataJson;
        e.createdAt = Instant.now();

        audit.save(e);
    }
}
