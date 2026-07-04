package com.ticket.master.ticket.kafka;

import com.ticket.master.common.kafka.EventCreatedMessage;
import com.ticket.master.common.kafka.OrderPaidMessage;
import com.ticket.master.common.kafka.TicketUnlockMessage;
import com.ticket.master.common.kafka.EventDeletedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import com.ticket.master.ticket.service.TicketService;
import com.ticket.master.common.kafka.ReserveTicketsMessage;
import com.ticket.master.common.kafka.CancelTicketsReservationMessage;
import com.ticket.master.common.kafka.ConfirmTicketsSoldMessage;

@Component
@RequiredArgsConstructor
@Slf4j
public class TicketKafkaListener {
    private final TicketService service;

    @KafkaListener(
            topics = "event-created-topic",
            groupId = "ticket-group"
    )

    public void handleEventCreated(EventCreatedMessage message){
        log.info("Recieved Kafka message about the new event: {}", message);

        try{
            service.generateTicketsFromSchema(message);
        }catch (Exception e){
            log.error("Critical error appeared while generating tickets for event {}: {}", message.eventId(), e.getMessage());
        }
    }

    @KafkaListener(
            topics = "order-paid-topic",
            groupId = "ticket-group"
    )
    public void handleOrderPaid(OrderPaidMessage message) {
        log.info("Received Kafka message about paid order: {}", message.orderId());
        try {
            service.markTicketsAsSold(message.ticketIds(), message.userId());
        } catch (Exception e) {
            log.error("Error processing payment for order {}: {}", message.orderId(), e.getMessage());
        }
    }

    @KafkaListener(
            topics = "ticket-unlock-topic",
            groupId = "ticket-group"
    )
    public void handleTicketUnlock(TicketUnlockMessage message) {
        log.info("Received Kafka message about the ticket unlock: {}", message);
        try {
            service.unlockTicket(message.ticketId());
        } catch (Exception e) {
            log.error("Error occurred while unlocking ticket {}: {}", message.ticketId(), e.getMessage());
        }
    }

    @KafkaListener(
            topics = "event-deleted-topic",
            groupId = "ticket-group"
    )
    public void handleEventDeleted(EventDeletedMessage message) {
        log.info("Received Kafka message about event deletion: {}", message.eventId());
        try {
            service.deleteTicketsByEventId(message.eventId());
        } catch (Exception e) {
            log.error("Error occurred while deleting tickets for event {}: {}", message.eventId(), e.getMessage());
        }
    }

    @KafkaListener(
            topics = "reserve-tickets-topic",
            groupId = "ticket-group"
    )
    public void handleReserveTickets(ReserveTicketsMessage message) {
        log.info("Received Kafka message to reserve tickets: {}", message);
        try {
            service.reserveTickets(message);
        } catch (Exception e) {
            log.error("Error occurred while reserving tickets for order {}: {}", message.orderId(), e.getMessage());
        }
    }
    
    @KafkaListener(
            topics = "cancel-tickets-reservation-topic",
            groupId = "ticket-group"
    )
    public void handleCancelTicketsReservation(CancelTicketsReservationMessage message) {
        log.info("Received Kafka message to cancel tickets reservation: {}", message);
        try {
            service.cancelReservation(message);
        } catch (Exception e) {
            log.error("Error occurred while canceling ticket reservation for order {}: {}", message.orderId(), e.getMessage());
        }
    }

    @KafkaListener(
            topics = "confirm-tickets-sold-topic",
            groupId = "ticket-group"
    )
    public void handleConfirmTicketsSold(ConfirmTicketsSoldMessage message) {
        log.info("Received Kafka message to confirm tickets sold: {}", message);
        try {
            service.confirmTicketsSold(message);
        } catch (Exception e) {
            log.error("Error occurred while confirming tickets sold for order {}: {}", message.orderId(), e.getMessage());
        }
    }
}
