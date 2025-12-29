package com.opencore.auth.api.dto;

public record SessionRevokeRequest(
        String refreshToken
) {}
