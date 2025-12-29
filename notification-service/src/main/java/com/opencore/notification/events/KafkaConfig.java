package com.opencore.notification.events;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {
    @Bean
    public ConsumerFactory<String, CloudEventEnvelope> consumerFactory(KafkaProperties props) {
        Map<String, Object> cfg = new HashMap<>(props.buildConsumerProperties());
        cfg.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        cfg.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        cfg.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        cfg.put(JsonDeserializer.VALUE_DEFAULT_TYPE, CloudEventEnvelope.class.getName());
        return new DefaultKafkaConsumerFactory<>(cfg, new StringDeserializer(), new JsonDeserializer<>(CloudEventEnvelope.class, false));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, CloudEventEnvelope> kafkaListenerContainerFactory(
            ConsumerFactory<String, CloudEventEnvelope> cf
    ) {
        ConcurrentKafkaListenerContainerFactory<String, CloudEventEnvelope> f = new ConcurrentKafkaListenerContainerFactory<>();
        f.setConsumerFactory(cf);
        return f;
    }
}
