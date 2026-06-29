package com.ticket.master.booking.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderCreateRequest(
        @NotNull UUID userId,
        @NotNull UUID eventId,
        @NotNull @Positive BigDecimal totalAmount,
        List<UUID> ticketIds
) {}
