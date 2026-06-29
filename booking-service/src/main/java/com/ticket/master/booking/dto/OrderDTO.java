package com.ticket.master.booking.dto;

import com.ticket.master.booking.model.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderDTO(
        UUID id,
        UUID userId,
        UUID eventId,
        BigDecimal totalAmount,
        OrderStatus status,
        LocalDateTime createdAt
) {}
