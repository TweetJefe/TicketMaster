package com.ticket.master.event.dto;

import jakarta.validation.Valid;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record EventDTO(
         UUID id,
         String name,
         String address,
         String city,
         Instant time,
         HallResponse hall,
    @Valid
         Set<PerformerResponse> performers,
         Set<CategoryResponseDTO> categories
) {}
