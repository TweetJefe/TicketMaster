package com.ticket.master.event.dto;

import java.util.UUID;

public record PerformerResponse(
        UUID id,
        String name,
        String genre,
        String description
) {
}
