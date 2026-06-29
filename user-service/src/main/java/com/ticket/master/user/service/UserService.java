package com.ticket.master.user.service;

import com.ticket.master.user.dto.AuthResponse;
import com.ticket.master.user.dto.UserDTO;
import com.ticket.master.user.dto.UserLoginRequest;
import com.ticket.master.user.dto.UserRegisterRequest;

import java.util.Optional;
import java.util.UUID;

public interface UserService {
    UserDTO register(UserRegisterRequest request);

    AuthResponse login(UserLoginRequest request);

    void grantAdminRole(UUID id);

    void grantOrganizationRole(UUID id);

    UserDTO getUserProfileByEmail(String username);
}
