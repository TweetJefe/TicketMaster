package com.ticket.master.event.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UpdateEventDTO(
        String name,
        String address,
        String city,

        @Future
        Instant time,

        UUID hallId,

        List<UUID> performerIds,

        @Valid
        List<CategoryRequestDTO> categories
) {}
