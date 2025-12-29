package com.opencore.auth.api.dto;

public record OtpVerifyRequest(
        String email,
        String otp,
        String requestId,
        String deviceId
) {}
