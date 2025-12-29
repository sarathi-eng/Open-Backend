package com.opencore.auth.api.dto;

public record OtpRequestRequest(
        String email,
        String deviceId
) {}
