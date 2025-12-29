package com.opencore.billing.api.dto;

public record SubscriptionExpiredRequest(
        String userId,
        String orgId,
        String subscriptionId,
        String reason
) {}
