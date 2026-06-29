package com.ticket.master.event.dto;

import jakarta.validation.constraints.NotBlank;

public record PerformerRequest(
        @NotBlank(message = "Name cannot be blank")
        String name,

        @NotBlank(message = "Genre cannot be blank")
        String genre,

        String description
) {
}
