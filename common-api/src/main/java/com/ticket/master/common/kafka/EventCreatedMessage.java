package com.ticket.master.common.kafka;

import com.ticket.master.common.enums.TicketType;
import com.ticket.master.common.scheme.TicketCategory;

import java.util.List;
import java.util.UUID;

public record EventCreatedMessage (
        UUID eventId,
        List<TicketCategory> categories
) {}