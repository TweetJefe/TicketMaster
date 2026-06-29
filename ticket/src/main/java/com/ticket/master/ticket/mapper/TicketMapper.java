package com.ticket.master.ticket.mapper;

import com.ticket.master.ticket.dto.TicketDTO;
import com.ticket.master.ticket.model.Ticket;
import org.springframework.stereotype.Component;

@Component
public class TicketMapper {
    public Ticket toEntity (TicketDTO dto){
        if (dto == null) return null;

        Ticket ticket = new Ticket();
        ticket.setId(dto.id());
        ticket.setPrice(dto.price());
        ticket.setEventId(dto.eventId());
        ticket.setSector(dto.sector());
        ticket.setRow(dto.row());
        ticket.setSeat(dto.seat());
        ticket.setStatus(dto.status());
        ticket.setType(dto.type());

        return ticket;
    }

    public TicketDTO toDTO (Ticket ticket){
        if (ticket == null) return null;

        return new TicketDTO(
                ticket.getId(),
                ticket.getPrice(),
                ticket.getEventId(),
                ticket.getUserId(),
                ticket.getSector(),
                ticket.getRow(),
                ticket.getSeat(),
                ticket.getStatus(),
                ticket.getType()
        );
    }
}
