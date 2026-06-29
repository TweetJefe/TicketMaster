package com.ticket.master.user.dto;

import lombok.Builder;

@Builder
public record UserRegisteredEvent(
        String email,
        String role
) {
}
