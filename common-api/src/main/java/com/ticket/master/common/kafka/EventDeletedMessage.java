package com.ticket.master.common.kafka;

import com.ticket.master.common.scheme.TicketCategory;

import java.util.List;
import java.util.UUID;

public record EventDeletedMessage(
        UUID eventId
) {}
