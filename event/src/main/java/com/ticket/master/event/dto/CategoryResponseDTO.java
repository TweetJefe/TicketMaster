package com.ticket.master.event.dto;

import com.ticket.master.common.enums.TicketType;

import java.util.UUID;

public record CategoryResponseDTO(
        UUID id,
        TicketType type,
        Integer quantity,
        double price
) {}
