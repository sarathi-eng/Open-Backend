package com.opencore.billing.events;

public final class BillingTopics {
    private BillingTopics() {}

    public static final String PAYMENT_SUCCEEDED = "opencore.billing.v1.payment-succeeded";
    public static final String SUBSCRIPTION_EXPIRED = "opencore.billing.v1.subscription-expired";

    // Consumed from user-service
    public static final String USER_CREATED = "opencore.user.v1.user-created";
}
