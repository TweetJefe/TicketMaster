package com.ticket.master.event.kafka;


import com.ticket.master.common.kafka.EventCreatedMessage;
import com.ticket.master.common.kafka.EventDeletedMessage;
import com.ticket.master.common.kafka.EventUpdatedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventKafkaProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC_EVENT_CREATED = "event-created-topic";
    private static final String TOPIC_EVENT_UPDATED = "event-updated-topic";
    private static final String TOPIC_EVENT_DELETED = "event-deleted-topic";

    public void sendEventCreatedMessage(EventCreatedMessage message){
        log.info("Kafka sending message, event created: {}", message.eventId());

        kafkaTemplate.send(TOPIC_EVENT_CREATED, message.eventId().toString(), message);
    }

    public void sendEventUpdatedMessage(EventUpdatedMessage message){
        log.info("Kafka sending message, event updated: {}", message.eventId());

        kafkaTemplate.send(TOPIC_EVENT_UPDATED, message.eventId().toString(), message);
    }

    public void sendEventDeletedMessage (EventDeletedMessage message) {
        log.info("Kafka sending message, event deleted: {}", message.eventId());

        kafkaTemplate.send(TOPIC_EVENT_DELETED, message.eventId().toString(), message);
    }
}
