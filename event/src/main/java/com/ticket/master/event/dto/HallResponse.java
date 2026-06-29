package com.ticket.master.event.dto;

import java.util.UUID;

public record HallResponse(
        UUID id,
        String name,
        String city,
        String address,
        Integer capacity
) {
}
