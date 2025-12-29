package com.opencore.billing.api.dto;

public record PaymentSucceededRequest(
        String userId,
        String orgId,
        long amountCents,
        String currency,
        String externalPaymentId
) {}
