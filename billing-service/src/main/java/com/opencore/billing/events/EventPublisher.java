package com.opencore.billing.events;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
public class EventPublisher {
    private final KafkaTemplate<String, CloudEventEnvelope> kafka;

    public EventPublisher(KafkaTemplate<String, CloudEventEnvelope> kafka) {
        this.kafka = kafka;
    }

    public void publish(String topic, String type, String subject, String source, Map<String, Object> data) {
        CloudEventEnvelope env = new CloudEventEnvelope(
                "1.0",
                UUID.randomUUID().toString(),
                source,
                type,
                subject,
                Instant.now(),
                "application/json",
                data
        );
        kafka.send(topic, subject, env);
    }
}
