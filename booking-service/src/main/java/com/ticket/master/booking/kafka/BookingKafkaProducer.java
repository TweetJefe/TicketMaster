package com.ticket.master.booking.kafka;

import com.ticket.master.common.kafka.CartExpiredMessage;
import com.ticket.master.common.kafka.OrderPaidMessage;
import com.ticket.master.common.kafka.TicketUnlockMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingKafkaProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC_ORDER_PAID = "order-paid-topic";
    private static final String TOPIC_TICKET_UNLOCK = "ticket-unlock-topic";
    private static final String TOPIC_CART_EXPIRED = "cart-expired-topic";

    public void sendOrderPaidMessage(OrderPaidMessage message){
        log.info("Order {} been paid successfully", message.orderId());

        kafkaTemplate.send(TOPIC_ORDER_PAID, message.orderId().toString(), message);


        log.info("Signal for order {} was successfully sent!", message.orderId());
    }

    public void sendTicketUnlockMessage(TicketUnlockMessage message) {
        log.info("Ticket {} for event {} is unlocked", message.ticketId(), message.eventId());

        kafkaTemplate.send(TOPIC_TICKET_UNLOCK, message.eventId().toString(), message);
    }

    public void sendCartExpiredMessage(CartExpiredMessage message){
        log.info("");

        kafkaTemplate.send(TOPIC_CART_EXPIRED, message.userId().toString(), message);
    }
}
