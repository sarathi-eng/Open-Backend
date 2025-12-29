package com.opencore.user.api.dto;

public record AddMemberRequest(
        String orgId,
        String userId,
        String role
) {}
