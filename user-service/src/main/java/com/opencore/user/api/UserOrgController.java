package com.opencore.user.api;

import com.opencore.user.api.dto.*;
import com.opencore.user.core.UserOrgService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1")
public class UserOrgController {
    private final UserOrgService svc;

    public UserOrgController(UserOrgService svc) {
        this.svc = svc;
    }

    @PostMapping("/users")
    public ResponseEntity<CreateUserResponse> createUser(@RequestBody CreateUserRequest request) {
        UUID userId = svc.createUser(request.email());
        return ResponseEntity.ok(new CreateUserResponse(userId.toString()));
    }

    @GetMapping("/users/{userId}/orgs")
    public ResponseEntity<List<Map<String, Object>>> listOrgs(@PathVariable String userId) {
        return ResponseEntity.ok(svc.listOrganizationsForUser(UUID.fromString(userId)));
    }

    @PostMapping("/orgs")
    public ResponseEntity<CreateOrgResponse> createOrg(@RequestBody CreateOrgRequest request) {
        UUID orgId = svc.createOrganization(request.name(), UUID.fromString(request.ownerUserId()));
        return ResponseEntity.ok(new CreateOrgResponse(orgId.toString()));
    }

    @PostMapping("/orgs/members")
    public ResponseEntity<Void> addMember(@RequestBody AddMemberRequest request) {
        svc.addMember(UUID.fromString(request.orgId()), UUID.fromString(request.userId()), request.role());
        return ResponseEntity.ok().build();
    }
}
