package com.opencore.user.core;

import com.opencore.user.events.EventPublisher;
import com.opencore.user.events.EventTopics;
import com.opencore.user.persistence.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;

@Service
public class UserOrgService {
    private final UserRepository users;
    private final OrganizationRepository orgs;
    private final OrgMembershipRepository memberships;
    private final AuditLogger audit;
    private final EventPublisher events;

    public UserOrgService(
            UserRepository users,
            OrganizationRepository orgs,
            OrgMembershipRepository memberships,
            AuditLogger audit,
            EventPublisher events
    ) {
        this.users = users;
        this.orgs = orgs;
        this.memberships = memberships;
        this.audit = audit;
        this.events = events;
    }

    @Transactional
    public UUID createUser(String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase();
        if (normalized.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "email required");
        }

        Optional<UserEntity> existing = users.findByEmail(normalized);
        if (existing.isPresent()) {
            return existing.get().id;
        }

        UserEntity u = new UserEntity();
        u.id = UUID.randomUUID();
        u.email = normalized;
        u.createdAt = Instant.now();
        users.save(u);

        audit.log("UserCreated", "User", u.id.toString(), "{\"email\":\"" + normalized + "\"}");
        events.publish(
                EventTopics.USER_CREATED,
                "UserCreated",
                u.id.toString(),
                "user-service",
                Map.of(
                        "userId", u.id.toString(),
                        "email", normalized,
                        "createdAt", u.createdAt.toString()
                )
        );

        return u.id;
    }

    @Transactional
    public UUID createOrganization(String name, UUID ownerUserId) {
        if (ownerUserId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ownerUserId required");
        }
        String n = name == null ? "" : name.trim();
        if (n.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name required");
        }

        if (users.findById(ownerUserId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "owner user not found");
        }

        OrganizationEntity org = new OrganizationEntity();
        org.id = UUID.randomUUID();
        org.name = n;
        org.createdAt = Instant.now();
        orgs.save(org);

        OrgMembershipEntity m = new OrgMembershipEntity();
        m.id = new OrgMembershipId(org.id, ownerUserId);
        m.role = "Admin";
        m.createdAt = Instant.now();
        memberships.save(m);

        audit.log("OrgCreated", "Organization", org.id.toString(), "{\"name\":\"" + escape(n) + "\"}");
        events.publish(
                EventTopics.ORG_CREATED,
                "OrgCreated",
                org.id.toString(),
                "user-service",
                Map.of(
                        "orgId", org.id.toString(),
                        "name", org.name,
                        "ownerUserId", ownerUserId.toString(),
                        "createdAt", org.createdAt.toString()
                )
        );

        events.publish(
                EventTopics.ORG_MEMBER_ADDED,
                "OrgMemberAdded",
                org.id + ":" + ownerUserId,
                "user-service",
                Map.of(
                        "orgId", org.id.toString(),
                        "userId", ownerUserId.toString(),
                        "role", "Admin",
                        "createdAt", m.createdAt.toString()
                )
        );

        return org.id;
    }

    @Transactional
    public void addMember(UUID orgId, UUID userId, String role) {
        if (orgId == null || userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "orgId and userId required");
        }
        if (orgs.findById(orgId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "org not found");
        }
        if (users.findById(userId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found");
        }

        String r = role == null ? "User" : role.trim();
        if (r.isBlank()) r = "User";

        OrgMembershipEntity m = new OrgMembershipEntity();
        m.id = new OrgMembershipId(orgId, userId);
        m.role = r;
        m.createdAt = Instant.now();
        memberships.save(m);

        audit.log("OrgMemberAdded", "OrgMembership", orgId + ":" + userId, "{\"role\":\"" + escape(r) + "\"}");
        events.publish(
                EventTopics.ORG_MEMBER_ADDED,
                "OrgMemberAdded",
                orgId + ":" + userId,
                "user-service",
                Map.of(
                        "orgId", orgId.toString(),
                        "userId", userId.toString(),
                        "role", r,
                        "createdAt", m.createdAt.toString()
                )
        );
    }

    public List<Map<String, Object>> listOrganizationsForUser(UUID userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId required");
        }
        List<OrgMembershipEntity> ms = memberships.findAllByIdUserId(userId);
        if (ms.isEmpty()) return List.of();

        Map<UUID, String> orgNames = new HashMap<>();
        for (OrganizationEntity o : orgs.findAllById(ms.stream().map(m -> m.id.orgId).toList())) {
            orgNames.put(o.id, o.name);
        }

        List<Map<String, Object>> out = new ArrayList<>();
        for (OrgMembershipEntity m : ms) {
            out.add(Map.of(
                    "orgId", m.id.orgId.toString(),
                    "orgName", orgNames.getOrDefault(m.id.orgId, ""),
                    "role", m.role
            ));
        }
        return out;
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
