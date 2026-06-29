package com.ticket.master.event.dto;

import com.ticket.master.common.enums.TicketType;

public record CategoryRequestDTO(
        TicketType type,
        Integer quantity,
        double price
) {}