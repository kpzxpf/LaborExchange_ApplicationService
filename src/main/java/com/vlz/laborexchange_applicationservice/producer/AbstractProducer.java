package com.vlz.laborexchange_applicationservice.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractProducer<T> {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    protected void sendMessage(String topic, T message) {
        log.info("Sending event to topic: {}", topic);
        try {
            kafkaTemplate.send(topic,  objectMapper.writeValueAsString(message));
        } catch (Exception ex) {
            throw new RuntimeException("Kafka serialization error", ex);
        }
    }
}