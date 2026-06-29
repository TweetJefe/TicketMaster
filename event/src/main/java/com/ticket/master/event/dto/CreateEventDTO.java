package com.ticket.master.event.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CreateEventDTO(
        @NotBlank
        String name,

        @NotBlank
        String address,

        @NotBlank
        String city,

        @NotNull
        @Future
        Instant time,

        @NotNull
        UUID hallId,

        @NotEmpty(message = "At least one performer has to be assigned")
        List<UUID> performerIds,

        @NotEmpty(message = "At least one type of ticket has to be created")
        @Valid
        List<CategoryRequestDTO> categories
) {}
