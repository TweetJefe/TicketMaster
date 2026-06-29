package com.ticket.master.user.kafka;

import com.ticket.master.user.dto.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaUserEventProducer implements UserEventProducer{
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC_USER_REGISTRATION = "user-registration-topic";

    @Override
    public void sendRegistrationEvent(UserRegisteredEvent event) {
        log.info("Sending registration event to Kafka for user: {}", event.email());
        kafkaTemplate.send(TOPIC_USER_REGISTRATION, event);
    }
}
