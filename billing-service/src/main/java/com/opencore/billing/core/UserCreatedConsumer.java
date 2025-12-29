package com.opencore.billing.core;

import com.opencore.billing.events.BillingTopics;
import com.opencore.billing.events.CloudEventEnvelope;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class UserCreatedConsumer {
    private static final Logger log = LoggerFactory.getLogger(UserCreatedConsumer.class);

    @KafkaListener(topics = BillingTopics.USER_CREATED, groupId = "billing-service")
    public void onUserCreated(CloudEventEnvelope event) {
        // Placeholder for creating billing customer/account.
        log.info("consumed event type={} subject={} source={} data={}", event.type(), event.subject(), event.source(), event.data());
    }
}
