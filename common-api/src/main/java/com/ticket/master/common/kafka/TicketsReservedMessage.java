package com.ticket.master.common.kafka;

import java.util.List;
import java.util.UUID;

public record TicketsReservedMessage(
        UUID orderId,
        List<UUID> ticketIds
) {}
