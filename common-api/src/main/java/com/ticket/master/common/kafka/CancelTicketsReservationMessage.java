package com.ticket.master.common.kafka;

import java.util.List;
import java.util.UUID;
public record CancelTicketsReservationMessage(
        UUID orderId,
        List<UUID> ticketIds
) {}
