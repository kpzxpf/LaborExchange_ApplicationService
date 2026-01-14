package com.vlz.laborexchange_applicationservice.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vlz.laborexchange_applicationservice.dto.ApplicationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ApplicationNotificationProducer extends AbstractProducer<ApplicationEvent> {
    @Value("${spring.kafka.topics.application-topic}")
    private String userRegistrationTopicName;

    public ApplicationNotificationProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        super(kafkaTemplate, objectMapper);
    }

    public void send(ApplicationEvent event) {
        super.sendMessage(userRegistrationTopicName, event);
    }
}