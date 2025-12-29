package com.opencore.user.api.audit;

import com.opencore.user.persistence.AuditLogEntity;
import com.opencore.user.persistence.AuditLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/audit")
public class AuditController {
    private final AuditLogRepository audit;

    public AuditController(AuditLogRepository audit) {
        this.audit = audit;
    }

    @GetMapping("/orgs/{orgId}")
    public ResponseEntity<List<Map<String, Object>>> listOrgAudit(
            @PathVariable String orgId,
            @RequestParam(defaultValue = "50") int limit
    ) {
        int clamped = Math.max(1, Math.min(limit, 200));
        var page = audit.findAllByOrgId(
                UUID.fromString(orgId),
                PageRequest.of(0, clamped, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        var out = page.getContent().stream().map(this::toDto).toList();
        return ResponseEntity.ok(out);
    }

    private Map<String, Object> toDto(AuditLogEntity e) {
        return Map.of(
                "id", e.id.toString(),
                "orgId", e.orgId == null ? null : e.orgId.toString(),
                "actorUserId", e.actorUserId == null ? null : e.actorUserId.toString(),
                "action", e.action,
                "resourceType", e.resourceType,
                "resourceId", e.resourceId,
                "ip", e.ip,
                "metadataJson", e.metadataJson,
                "createdAt", e.createdAt.toString()
        );
    }
}
