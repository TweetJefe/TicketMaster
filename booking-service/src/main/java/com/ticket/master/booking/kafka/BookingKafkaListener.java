package com.ticket.master.booking.kafka;

import com.ticket.master.common.kafka.EventDeletedMessage;
import com.ticket.master.common.kafka.TicketsReservationFailedMessage;
import com.ticket.master.common.kafka.TicketsReservedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import com.ticket.master.booking.redis.CartService;
import com.ticket.master.booking.service.OrderService;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingKafkaListener {
    private final OrderService orderService;
    private final CartService cartService;

    @KafkaListener(topics = "event-deleted-topic", groupId = "booking-group")
    public void handleEventDeleted(EventDeletedMessage message){
        log.info("Event delete signal is received. Event with ID {} is deleted, starting the booking cancellation", message.eventId());

        try{
            orderService.cancelOrdersByEventId(message.eventId());

            cartService.removeEventFromAllCarts(message.eventId());

            log.info("All orders and bookings for the event with ID {} are cancelled", message.eventId());
        }catch (Exception e){
            log.error("Error occurred while execution of order/booking cancellation for event with ID {}, {}", message.eventId(), e.getMessage());
        }
    }

    @KafkaListener(topics = "tickets-reserved-topic", groupId = "booking-group")
    public void handleTicketsReserved(TicketsReservedMessage message) {
        log.info("Saga step succeeded: tickets successfully reserved in DB for order {}", message.orderId());
    }


    @KafkaListener(topics = "tickets-reservation-failed-topic", groupId = "booking-group")
    public void handleTicketsReservationFailed(TicketsReservationFailedMessage message) {
        log.warn("Saga step failed: tickets reservation failed for order {}. Reason: {}. Starting compensation...",
                message.orderId(), message.reason());
        try {
            orderService.cancelOrder(message.orderId(), false);
        } catch (Exception e) {
            log.error("Error executing compensating actions for order {}: {}", message.orderId(), e.getMessage());
        }
    }
}
