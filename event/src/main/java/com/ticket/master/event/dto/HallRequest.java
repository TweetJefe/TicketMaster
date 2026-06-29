package com.ticket.master.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record HallRequest(
        @NotBlank(message = "Name cannot be blank")
        String name,

        @NotBlank(message = "City cannot be blank")
        String city,

        @NotBlank(message = "Address cannot be blank")
        String address,

        @NotNull(message = "Capacity cannot be null")
        @Positive(message = "Capacity must be positive")
        Integer capacity
) {
}
