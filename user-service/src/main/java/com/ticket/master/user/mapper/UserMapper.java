package com.ticket.master.user.mapper;

import com.ticket.master.user.dto.UserDTO;
import com.ticket.master.user.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserDTO toDto(User user) {
        if (user == null) return null;
        return new UserDTO(
                user.getId(),
                user.getEmail(),
                user.getRole()
        );
    }
}
