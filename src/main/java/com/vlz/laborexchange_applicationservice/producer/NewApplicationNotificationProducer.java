package com.vlz.laborexchange_applicationservice.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vlz.laborexchange_applicationservice.dto.NewApplicationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NewApplicationNotificationProducer extends AbstractProducer<NewApplicationEvent> {
    @Value("${spring.kafka.topics.new-topic}")
    private String newApplicationNotificationTopicName;

    public NewApplicationNotificationProducer(KafkaTemplate<String,
            String> kafkaTemplate, ObjectMapper objectMapper) {
        super(kafkaTemplate, objectMapper);
    }

    public void send(NewApplicationEvent event) {
        super.sendMessage(newApplicationNotificationTopicName, event);
        log.info("New application notification has been sent VacancyTitle {}", event.getVacancyTitle());
    }
}