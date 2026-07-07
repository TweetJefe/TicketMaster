package com.ticket.master.common.kafka;

import java.util.List;
import java.util.UUID;
public record ConfirmTicketsSoldMessage(
        UUID orderId,
        UUID userId,
        List<UUID> ticketIds
) {}