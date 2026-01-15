package com.vlz.laborexchange_applicationservice.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vlz.laborexchange_applicationservice.dto.RejectedApplicationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RejectedApplicationNotificationProducer extends AbstractProducer<RejectedApplicationEvent> {
    @Value("${spring.kafka.topics.rejected-topic}")
    private String rejectedApplicationNotificationTopicName;

    public RejectedApplicationNotificationProducer(KafkaTemplate<String,
            String> kafkaTemplate, ObjectMapper objectMapper) {
        super(kafkaTemplate, objectMapper);
    }

    public void send(RejectedApplicationEvent event) {
        super.sendMessage(rejectedApplicationNotificationTopicName, event);
        log.info("Rejected application notification has been sent VacancyTitle {}", event.getVacancyTitle());
    }
}
