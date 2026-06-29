package com.ticket.master.common.kafka;

import java.util.UUID;

public record TicketUnlockMessage(
        UUID ticketId,
        UUID eventId
) {
}
