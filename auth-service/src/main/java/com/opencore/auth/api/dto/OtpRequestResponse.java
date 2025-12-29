package com.opencore.auth.api.dto;

public record OtpRequestResponse(
        String requestId,
        // DEV convenience. In production this should be delivered out-of-band.
        String devOtp
) {}
