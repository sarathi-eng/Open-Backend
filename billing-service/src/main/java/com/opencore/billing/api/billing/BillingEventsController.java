package com.opencore.billing.api.billing;

import com.opencore.billing.api.dto.PaymentSucceededRequest;
import com.opencore.billing.api.dto.SubscriptionExpiredRequest;
import com.opencore.billing.events.BillingTopics;
import com.opencore.billing.events.EventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/billing")
public class BillingEventsController {
    private final EventPublisher events;

    public BillingEventsController(EventPublisher events) {
        this.events = events;
    }

    @PostMapping("/payments/succeeded")
    public ResponseEntity<Void> paymentSucceeded(@RequestBody PaymentSucceededRequest request) {
        events.publish(
                BillingTopics.PAYMENT_SUCCEEDED,
                "PaymentSucceeded",
                request.externalPaymentId() == null ? "" : request.externalPaymentId(),
                "billing-service",
                Map.of(
                        "userId", request.userId(),
                        "orgId", request.orgId(),
                        "amountCents", request.amountCents(),
                        "currency", request.currency(),
                        "externalPaymentId", request.externalPaymentId()
                )
        );
        return ResponseEntity.ok().build();
    }

    @PostMapping("/subscriptions/expired")
    public ResponseEntity<Void> subscriptionExpired(@RequestBody SubscriptionExpiredRequest request) {
        events.publish(
                BillingTopics.SUBSCRIPTION_EXPIRED,
                "SubscriptionExpired",
                request.subscriptionId() == null ? "" : request.subscriptionId(),
                "billing-service",
                Map.of(
                        "userId", request.userId(),
                        "orgId", request.orgId(),
                        "subscriptionId", request.subscriptionId(),
                        "reason", request.reason()
                )
        );
        return ResponseEntity.ok().build();
    }
}
