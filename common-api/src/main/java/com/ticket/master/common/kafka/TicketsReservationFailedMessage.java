package com.ticket.master.common.kafka;

import java.util.UUID;
public record TicketsReservationFailedMessage(
        UUID orderId,
        String reason
) {}
