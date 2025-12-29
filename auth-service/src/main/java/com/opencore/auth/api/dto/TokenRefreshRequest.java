package com.opencore.auth.api.dto;

public record TokenRefreshRequest(
        String refreshToken
) {}
