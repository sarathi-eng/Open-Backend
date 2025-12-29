package com.opencore.billing.events;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {
    @Bean
    public ProducerFactory<String, CloudEventEnvelope> producerFactory(KafkaProperties props) {
        Map<String, Object> cfg = new HashMap<>(props.buildProducerProperties());
        cfg.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        cfg.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        cfg.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaProducerFactory<>(cfg);
    }

    @Bean
    public KafkaTemplate<String, CloudEventEnvelope> kafkaTemplate(ProducerFactory<String, CloudEventEnvelope> pf) {
        return new KafkaTemplate<>(pf);
    }

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

    @Bean
    public NewTopic paymentSucceededTopic() {
        return TopicBuilder.name(BillingTopics.PAYMENT_SUCCEEDED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic subscriptionExpiredTopic() {
        return TopicBuilder.name(BillingTopics.SUBSCRIPTION_EXPIRED).partitions(3).replicas(1).build();
    }
}
