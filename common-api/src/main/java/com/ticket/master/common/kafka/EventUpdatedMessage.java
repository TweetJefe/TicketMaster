package com.ticket.master.common.kafka;

import jakarta.validation.Valid;
import com.ticket.master.common.scheme.TicketCategory;

import java.util.List;
import java.util.UUID;

public record EventUpdatedMessage (
        UUID eventId,
        List<TicketCategory> categories
) {
}
