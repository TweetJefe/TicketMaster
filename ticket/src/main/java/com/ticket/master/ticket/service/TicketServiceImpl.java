package com.ticket.master.ticket.service;

import com.ticket.master.common.kafka.*;
import com.ticket.master.ticket.dto.kafka.BuyTicketRequest;
import com.ticket.master.ticket.dto.TicketDTO;
import com.ticket.master.ticket.enums.Status;
import com.ticket.master.common.enums.TicketType;
import com.ticket.master.common.exception.NullableViolation;
import com.ticket.master.common.exception.PostgresErrorCodes;
import com.ticket.master.common.exception.ServerException;
import com.ticket.master.common.exception.UniquenessViolation;
import com.ticket.master.ticket.kafka.TicketKafkaProducer;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.ticket.master.ticket.mapper.TicketMapper;
import com.ticket.master.ticket.model.Ticket;
import org.apache.kafka.common.metrics.Stat;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.ticket.master.ticket.repository.TicketRepository;
import com.ticket.master.common.scheme.TicketCategory;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.ticket.master.common.enums.TicketType;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService{
    private final TicketRepository repository;
    private final TicketMapper mapper;
    private final TicketKafkaProducer ticketKafkaProducer;

    @Override
    public void generateTicketsFromSchema(EventCreatedMessage message) {
        List<Ticket> ticketToSave = new ArrayList<>();

        for (TicketCategory category : message.categories()){
            for (int i = 1; i <= category.quantity(); i++){
                Ticket ticket = new Ticket();
                ticket.setEventId(message.eventId());
                ticket.setPrice(category.price());
                ticket.setType(category.type());
                ticket.setSeat(String.valueOf(i));
                ticket.setStatus(Status.AVAILABLE);

                ticketToSave.add(ticket);
            }
        }

        repository.saveAll(ticketToSave);
        log.info("tickets generated for event: {}", message.eventId());
    }

    @Override
    public Ticket saveTicket(Ticket ticket) {
        try {
            return repository.save(ticket);
        }catch (DataIntegrityViolationException exception){
            Throwable cause = exception.getCause();
            if(cause instanceof ConstraintViolationException cve){
                String sqlState = cve.getSQLState();
                if(sqlState.equals(PostgresErrorCodes.UNIQUE_VIOLATION)){
                    String constraintName =cve.getConstraintName();
                    throw new UniquenessViolation(constraintName);
                }else if(sqlState.equals(PostgresErrorCodes.NOT_NULL_VIOLATION)){
                    throw new NullableViolation("Nullable Violation");
                }
            }else{
                throw new ServerException();
            }
        }
        return null;
    }

    @Override
    @Transactional
    public TicketDTO buyTicket(BuyTicketRequest dto, UUID id) {
        Ticket ticket = repository.findById(id)
                .orElseThrow(EntityNotFoundException::new);
        if (ticket.getUserId() != null) {
            throw new IllegalStateException("Ticket is already sold");
        }

        ticket.setUserId(dto.userId());
        ticket.setStatus(Status.SOLD);

        saveTicket(ticket);

        return mapper.toDTO(ticket);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = {IllegalStateException.class})
    public void markTicketsAsSold(List<UUID> uuids, UUID uuid) {
        for(var ticketId : uuids){
            Ticket ticket = repository.findById(ticketId)
                    .orElseThrow(() -> new EntityNotFoundException("Ticket not found"));
            if(ticket.getStatus() == Status.SOLD){
                throw new IllegalStateException("Those tickets are unavailable");
            }else{
                ticket.setStatus(Status.SOLD);
                ticket.setUserId(uuid);
                saveTicket(ticket);
            }
        }
    }

    @Override
    @Transactional
    public void unlockTicket(UUID id) {
        Ticket ticket = repository.findById(id)
                .orElse(null);
        if(ticket != null){
            ticket.setStatus(Status.AVAILABLE);
            ticket.setUserId(null);
            saveTicket(ticket);
            log.info("Ticket {} has been unlocked and is now AVAILABLE", id);
        }else{
            log.warn("Attempted to unlock non-existent ticket with ID: {}", id);
        }
    }

    @Override
    @Transactional
    public void deleteTicketsByEventId(UUID eventId) {
        log.info("Deleting all tickets for event: {}", eventId);
        repository.deleteByEventId(eventId);
    }

    @Override
    public void reserveTickets(ReserveTicketsMessage message) {
        log.info("Processing ticket reservation for order: {}", message.orderId());
        List<Ticket> tickets = new ArrayList<>();
        boolean rAnyUnavailable = false;

        for (UUID ticketId : message.ticketIds()){
            Ticket ticket = repository.findById(ticketId).orElse(null);
            if (ticket == null || ticket.getStatus() != Status.AVAILABLE){
                rAnyUnavailable = true;
                break;
            }
            tickets.add(ticket);
        }
        if (rAnyUnavailable){
            log.warn("Reservation failed for order {}: some tickets are unavailable", message.orderId());
            ticketKafkaProducer.sendTicketsReservationFailedMessage(new TicketsReservationFailedMessage(
                    message.orderId(),
                    "Some tickets are already locked or sold"));
            return;
        }

        for (Ticket ticket : tickets){
            ticket.setStatus(Status.LOCKED);
            saveTicket(ticket);
        }
        log.info("Tickets successfully locked for order {}", message.orderId());
        ticketKafkaProducer.sendTicketsReservedMessage(
                new TicketsReservedMessage(
                        message.orderId(),
                        message.ticketIds()
                )
        );
    }

    @Override
    public void cancelReservation(CancelTicketsReservationMessage message) {
        log.info("Canceling ticket reservation for order: {}", message.orderId());
        for (UUID ticketId : message.ticketIds()){
            Ticket ticket = repository.findById(ticketId).orElse(null);
            if (ticket != null && ticket.getStatus() == Status.LOCKED){
                ticket.setStatus(Status.AVAILABLE);
                ticket.setUserId(null);
                saveTicket(ticket);
            }
        }
        log.info("Tickets unlocked for order {}", message.orderId());
    }

    @Override
    public void confirmTicketsSold(ConfirmTicketsSoldMessage message) {
        log.info("Confirming tickets sold for order: {}", message.orderId());
        for (UUID ticketId : message.ticketIds()){
            Ticket ticket = repository.findById(ticketId).orElse(null);
            if (ticket != null){
                ticket.setStatus(Status.SOLD);
                ticket.setUserId(message.userId());
                saveTicket(ticket);
            }
        }
        log.info("Tickets status updated to SOLD for order {}", message.orderId());
    }
}
