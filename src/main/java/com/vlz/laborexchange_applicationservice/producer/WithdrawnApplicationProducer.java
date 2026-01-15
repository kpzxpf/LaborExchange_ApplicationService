package com.vlz.laborexchange_applicationservice.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vlz.laborexchange_applicationservice.dto.WithdrawnApplicationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WithdrawnApplicationProducer extends AbstractProducer<WithdrawnApplicationEvent> {
    @Value("${spring.kafka.topics.withdrawn-topic}")
    private String withdrawnApplicationNotificationTopicName;

    public WithdrawnApplicationProducer(
            KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        super(kafkaTemplate, objectMapper);
    }

    public void send(WithdrawnApplicationEvent event) {
        super.sendMessage(withdrawnApplicationNotificationTopicName, event);
        log.info("Withdrawn application notification has been sent VacancyTitle {}", event.getVacancyTitle());
    }
}
