package com.vlz.laborexchange_applicationservice.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vlz.laborexchange_applicationservice.dto.AcceptedApplicationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AcceptedApplicationNotificationProducer extends AbstractProducer<AcceptedApplicationEvent> {

    @Value("${spring.kafka.topics.accepted-topic}")
    private String acceptedTopicName;

    public AcceptedApplicationNotificationProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        super(kafkaTemplate, objectMapper);
    }

    public void send(AcceptedApplicationEvent event) {
        super.sendMessage(acceptedTopicName, event);
        log.info("Accepted application notification sent for vacancy: {}", event.getVacancyTitle());
    }
}
