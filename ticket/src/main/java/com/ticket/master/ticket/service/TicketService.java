package com.ticket.master.ticket.service;

import com.ticket.master.ticket.dto.kafka.BuyTicketRequest;
import com.ticket.master.ticket.dto.CreateTicketDTO;
import com.ticket.master.ticket.dto.TicketDTO;
import com.ticket.master.common.kafka.EventCreatedMessage;
import com.ticket.master.ticket.model.Ticket;

import java.util.List;
import java.util.UUID;

public interface TicketService {

    void generateTicketsFromSchema(EventCreatedMessage message);

    Ticket saveTicket (Ticket ticket);

    TicketDTO buyTicket(BuyTicketRequest dto, UUID id);

    void markTicketsAsSold(List<UUID> uuids, UUID uuid);

    void unlockTicket(UUID id);
}
