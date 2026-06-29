package com.ticket.master.user.controller;

import com.ticket.master.user.dto.UserDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import com.ticket.master.user.service.UserService;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/role/admin")
    public ResponseEntity<Void> makeUserAdmin(@PathVariable UUID id){
        userService.grantAdminRole(id);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/role/organizer")
    public ResponseEntity<Void> makeUserOrganizer(@PathVariable UUID id){
        userService.grantOrganizationRole(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getMyProfile(@AuthenticationPrincipal UserDetails userDetails){
        UserDTO profile = userService.getUserProfileByEmail(userDetails.getUsername());
        return ResponseEntity.ok(profile);
    }
}
