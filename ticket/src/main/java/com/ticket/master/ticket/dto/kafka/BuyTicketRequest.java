package com.ticket.master.ticket.dto.kafka;

import java.util.UUID;

public record BuyTicketRequest(
        UUID ticketId,
        UUID userId
) {
}
