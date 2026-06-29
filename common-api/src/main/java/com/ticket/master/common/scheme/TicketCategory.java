package com.ticket.master.common.scheme;

import com.ticket.master.common.enums.TicketType;

public record TicketCategory(
        TicketType type,
        Integer quantity,
        double price
) {
}
