package com.ticket.master.common.kafka;

import java.util.List;
import java.util.UUID;




public record OrderPaidMessage(
        UUID orderId,
        UUID userId,
        List<UUID> ticketIds
) {}
