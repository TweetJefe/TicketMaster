package com.ticket.master.ticket.service;

import com.ticket.master.ticket.dto.kafka.BuyTicketRequest;
import com.ticket.master.ticket.dto.TicketDTO;
import com.ticket.master.ticket.enums.Status;
import com.ticket.master.common.enums.TicketType;
import com.ticket.master.common.exception.NullableViolation;
import com.ticket.master.common.exception.PostgresErrorCodes;
import com.ticket.master.common.exception.ServerException;
import com.ticket.master.common.exception.UniquenessViolation;
import jakarta.persistence.EntityNotFoundException;
import com.ticket.master.common.kafka.EventCreatedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.ticket.master.ticket.mapper.TicketMapper;
import com.ticket.master.ticket.model.Ticket;
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
            if(ticket.getStatus() != Status.AVAILABLE){
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
}
