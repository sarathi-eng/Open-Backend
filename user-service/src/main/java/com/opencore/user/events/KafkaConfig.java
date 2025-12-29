package com.opencore.user.events;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
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
    public NewTopic userCreatedTopic() {
        return TopicBuilder.name(EventTopics.USER_CREATED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic orgCreatedTopic() {
        return TopicBuilder.name(EventTopics.ORG_CREATED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic orgMemberAddedTopic() {
        return TopicBuilder.name(EventTopics.ORG_MEMBER_ADDED).partitions(3).replicas(1).build();
    }
}
