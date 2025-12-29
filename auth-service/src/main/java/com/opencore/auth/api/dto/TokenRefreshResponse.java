package com.opencore.auth.api.dto;

public record TokenRefreshResponse(
        String accessToken,
        String refreshToken,
        long accessExpiresInSeconds
) {}
