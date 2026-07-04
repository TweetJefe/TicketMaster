package com.ticket.master.ticket.kafka;

import com.ticket.master.common.kafka.TicketsReservationFailedMessage;
import com.ticket.master.common.kafka.TicketsReservedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TicketKafkaProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC_TICKETS_RESERVED = "tickets-reserved-topic";
    private static final String TOPIC_RESERVATION_FAILED = "tickets-reservation-failed-topic";

    public void sendTicketsReservedMessage(TicketsReservedMessage message) {
        log.info("Sending saga event: tickets reserved successfully for order {}", message.orderId());
        kafkaTemplate.send(TOPIC_TICKETS_RESERVED, message.orderId().toString(), message);
    }

    public void sendTicketsReservationFailedMessage(TicketsReservationFailedMessage message) {
        log.info("Sending saga event: tickets reservation failed for order {}", message.orderId());
        kafkaTemplate.send(TOPIC_RESERVATION_FAILED, message.orderId().toString(), message);
    }
}
