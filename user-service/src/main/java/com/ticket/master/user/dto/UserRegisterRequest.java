package com.ticket.master.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.ticket.master.user.model.Role;

public record UserRegisterRequest(
        @NotBlank(message = "Email can't be empty")
        @Email(message = "Incorrect email format")
        String email,

        @NotBlank(message = "Can't be empty")
        @Size(min = 8, message = "Password must be at least 8 characters long")
        String password
) {}
