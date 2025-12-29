package com.opencore.notification.core;

import com.opencore.notification.events.CloudEventEnvelope;
import com.opencore.notification.events.NotificationTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class EventConsumers {
    private static final Logger log = LoggerFactory.getLogger(EventConsumers.class);

    @KafkaListener(topics = NotificationTopics.USER_CREATED, groupId = "notification-service")
    public void onUserCreated(CloudEventEnvelope event) {
        log.info("notify: consumed event type={} subject={} source={} data={}", event.type(), event.subject(), event.source(), event.data());
    }

    @KafkaListener(topics = NotificationTopics.PAYMENT_SUCCEEDED, groupId = "notification-service")
    public void onPaymentSucceeded(CloudEventEnvelope event) {
        log.info("notify: consumed event type={} subject={} source={} data={}", event.type(), event.subject(), event.source(), event.data());
    }

    @KafkaListener(topics = NotificationTopics.SUBSCRIPTION_EXPIRED, groupId = "notification-service")
    public void onSubscriptionExpired(CloudEventEnvelope event) {
        log.info("notify: consumed event type={} subject={} source={} data={}", event.type(), event.subject(), event.source(), event.data());
    }
}
