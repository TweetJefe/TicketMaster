package com.ticket.master.user.controller;

import com.ticket.master.user.TestcontainersConfiguration;
import com.ticket.master.user.model.Role;
import com.ticket.master.user.model.User;
import com.ticket.master.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Nested
    class MakeUserAdmin {
        @Test
        @WithMockUser(roles = "ADMIN")
        void shouldGrantAdminRole() throws Exception {
            User user = User.builder()
                    .email("target@example.com")
                    .password(passwordEncoder.encode("Password123!"))
                    .role(Role.CUSTOMER)
                    .build();
            user = userRepository.save(user);

            mockMvc.perform(patch("/api/users/{id}/role/admin", user.getId()))
                    .andExpect(status().isOk());

            User updatedUser = userRepository.findById(user.getId()).orElseThrow();
            assert updatedUser.getRole() == Role.ADMIN;
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        void shouldReturnForbiddenForNonAdmin() throws Exception {
            User user = User.builder()
                    .email("target@example.com")
                    .password(passwordEncoder.encode("Password123!"))
                    .role(Role.CUSTOMER)
                    .build();
            user = userRepository.save(user);

            mockMvc.perform(patch("/api/users/{id}/role/admin", user.getId()))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    class MakeUserOrganizer {
        @Test
        @WithMockUser(roles = "ADMIN")
        void shouldGrantOrganizerRole() throws Exception {
            User user = User.builder()
                    .email("target@example.com")
                    .password(passwordEncoder.encode("Password123!"))
                    .role(Role.CUSTOMER)
                    .build();
            user = userRepository.save(user);

            mockMvc.perform(patch("/api/users/{id}/role/organizer", user.getId()))
                    .andExpect(status().isOk());

            User updatedUser = userRepository.findById(user.getId()).orElseThrow();
            assert updatedUser.getRole() == Role.ORGANIZATION;
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        void shouldReturnForbiddenForNonAdmin() throws Exception {
            User user = User.builder()
                    .email("target@example.com")
                    .password(passwordEncoder.encode("Password123!"))
                    .role(Role.CUSTOMER)
                    .build();
            user = userRepository.save(user);

            mockMvc.perform(patch("/api/users/{id}/role/organizer", user.getId()))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    class GetMyProfile {
        @Test
        @WithMockUser(username = "me@example.com", roles = "CUSTOMER")
        void shouldReturnUserProfile() throws Exception {
            User user = User.builder()
                    .email("me@example.com")
                    .password(passwordEncoder.encode("Password123!"))
                    .role(Role.CUSTOMER)
                    .build();
            userRepository.save(user);

            mockMvc.perform(get("/api/users/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("me@example.com"))
                    .andExpect(jsonPath("$.role").value("CUSTOMER"));
        }

        @Test
        void shouldReturnForbiddenIfNotAuthenticated() throws Exception {
            mockMvc.perform(get("/api/users/me"))
                    .andExpect(status().isForbidden());
        }
    }
}
