package com.ticket.master.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.master.user.TestcontainersConfiguration;
import com.ticket.master.user.dto.UserLoginRequest;
import com.ticket.master.user.dto.UserRegisterRequest;
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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Nested
    class Register {
        @Test
        void shouldRegisterNewUser() throws Exception {
            UserRegisterRequest request = new UserRegisterRequest("newuser@example.com", "Password123!");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.email").value("newuser@example.com"))
                    .andExpect(jsonPath("$.role").value("CUSTOMER"));
        }

        @Test
        void shouldReturnConflictIfEmailAlreadyExists() throws Exception {
            User user = User.builder()
                    .email("existing@example.com")
                    .password(passwordEncoder.encode("Password123!"))
                    .role(Role.CUSTOMER)
                    .build();
            userRepository.save(user);

            UserRegisterRequest request = new UserRegisterRequest("existing@example.com", "Password123!");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        void shouldReturnBadRequestIfEmailIsInvalid() throws Exception {
            UserRegisterRequest request = new UserRegisterRequest("invalid-email", "Password123!");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class Login {
        @Test
        void shouldLoginAndReturnToken() throws Exception {
            User user = User.builder()
                    .email("login@example.com")
                    .password(passwordEncoder.encode("Password123!"))
                    .role(Role.CUSTOMER)
                    .build();
            userRepository.save(user);

            UserLoginRequest request = new UserLoginRequest("login@example.com", "Password123!");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").exists());
        }

        @Test
        void shouldReturnUnauthorizedForBadCredentials() throws Exception {
            User user = User.builder()
                    .email("login@example.com")
                    .password(passwordEncoder.encode("Password123!"))
                    .role(Role.CUSTOMER)
                    .build();
            userRepository.save(user);

            UserLoginRequest request = new UserLoginRequest("login@example.com", "WrongPassword!");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }
}
