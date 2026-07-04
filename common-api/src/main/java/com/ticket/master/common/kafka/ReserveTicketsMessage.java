package com.ticket.master.common.kafka;

import java.util.List;
import java.util.UUID;
public record ReserveTicketsMessage(
        UUID orderId,
        UUID eventId,
        UUID userId,
        List<UUID> ticketIds
) {}
