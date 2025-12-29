package com.opencore.user.api.dto;

public record CreateOrgRequest(
        String name,
        String ownerUserId
) {}
