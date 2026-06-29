package com.ticket.master.event.dto;

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
         Set<PerformerResponse> performers,
         Set<CategoryResponseDTO> categories
) {}
