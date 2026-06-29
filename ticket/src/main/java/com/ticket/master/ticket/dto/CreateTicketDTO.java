package com.ticket.master.ticket.dto;
import com.ticket.master.common.enums.TicketType;
import com.ticket.master.ticket.enums.Status;

import java.util.UUID;

public record CreateTicketDTO(
        double price,
        UUID eventId,
        UUID userId,
        String sector,
        String row,
        String seat,
        Status status,
        TicketType type
) {
}
