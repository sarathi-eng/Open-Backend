package com.opencore.auth.core;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "opencore.jwt")
public record OpenCoreJwtProperties(
        String issuer,
        String secret,
        long accessTtlSeconds,
        long refreshTtlSeconds
) {}
