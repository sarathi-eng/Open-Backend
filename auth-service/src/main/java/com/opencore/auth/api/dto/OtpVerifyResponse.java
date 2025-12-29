package com.opencore.auth.api.dto;

public record OtpVerifyResponse(
        String accessToken,
        String refreshToken,
        long accessExpiresInSeconds
) {}
