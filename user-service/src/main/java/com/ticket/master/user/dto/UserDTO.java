package com.ticket.master.user.dto;

import com.ticket.master.user.model.Role;
import java.util.UUID;

public record UserDTO(
        UUID id,
        String email,
        Role role
) {}
