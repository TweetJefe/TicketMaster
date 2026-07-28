package com.ticket.master.user.service;

import com.ticket.master.user.dto.AuthResponse;
import com.ticket.master.user.dto.UserDTO;
import com.ticket.master.user.dto.UserLoginRequest;
import com.ticket.master.user.dto.UserRegisterRequest;
import com.ticket.master.user.dto.UserRegisteredEvent;
import com.ticket.master.user.exception.UserAlreadyExists;
import com.ticket.master.user.kafka.UserEventProducer;
import com.ticket.master.user.mapper.UserMapper;
import com.ticket.master.user.model.Role;
import com.ticket.master.user.model.User;
import com.ticket.master.user.repository.UserRepository;
import com.ticket.master.user.security.JwtUtils;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtils jwtUtils;

    @Spy
    private UserMapper mapper = new UserMapper();

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserEventProducer userEventProducer;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private UserServiceImpl userService;

    @Nested
    class Register {
        @Test
        void shouldRegisterSuccessfully() {
            UserRegisterRequest request = new UserRegisterRequest("test@example.com", "password123");
            User savedUser = User.builder()
                    .id(UUID.randomUUID())
                    .email("test@example.com")
                    .password("encoded_password")
                    .role(Role.CUSTOMER)
                    .build();
            UserDTO expectedDto = new UserDTO(savedUser.getId(), savedUser.getEmail(), savedUser.getRole());

            when(repository.existsByEmail("test@example.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encoded_password");
            when(repository.save(any(User.class))).thenReturn(savedUser);

            UserDTO result = userService.register(request);

            assertNotNull(result);
            assertEquals("test@example.com", result.email());
            assertEquals(Role.CUSTOMER, result.role());

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(repository).save(userCaptor.capture());
            User capturedUser = userCaptor.getValue();
            assertEquals("test@example.com", capturedUser.getEmail());
            assertEquals("encoded_password", capturedUser.getPassword());
            assertEquals(Role.CUSTOMER, capturedUser.getRole());

            ArgumentCaptor<UserRegisteredEvent> eventCaptor = ArgumentCaptor.forClass(UserRegisteredEvent.class);
            verify(userEventProducer).sendRegistrationEvent(eventCaptor.capture());
            UserRegisteredEvent capturedEvent = eventCaptor.getValue();
            assertEquals("test@example.com", capturedEvent.email());
            assertEquals("CUSTOMER", capturedEvent.role());
        }

        @Test
        void shouldThrowExceptionWhenUserAlreadyExists() {
            UserRegisterRequest request = new UserRegisterRequest("test@example.com", "password123");
            when(repository.existsByEmail("test@example.com")).thenReturn(true);

            assertThrows(UserAlreadyExists.class, () -> userService.register(request));

            verify(repository, never()).save(any(User.class));
            verify(userEventProducer, never()).sendRegistrationEvent(any(UserRegisteredEvent.class));
        }
    }

    @Nested
    class Login {
        @Test
        void shouldLoginSuccessfullyAndReturnToken() {
            UserLoginRequest request = new UserLoginRequest("test@example.com", "password123");
            Authentication authentication = mock(Authentication.class);
            UserDetails userDetails = mock(UserDetails.class);

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(userDetails);
            when(jwtUtils.generateToken(userDetails)).thenReturn("dummy.jwt.token");

            AuthResponse response = userService.login(request);

            assertNotNull(response);
            assertEquals("dummy.jwt.token", response.token());

            ArgumentCaptor<UsernamePasswordAuthenticationToken> tokenCaptor =
                    ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
            verify(authenticationManager).authenticate(tokenCaptor.capture());
            assertEquals("test@example.com", tokenCaptor.getValue().getPrincipal());
            assertEquals("password123", tokenCaptor.getValue().getCredentials());
        }
    }

    @Nested
    class GrantAdminRole {
        @Test
        void shouldGrantAdminRoleSuccessfully() {
            UUID userId = UUID.randomUUID();
            User user = User.builder()
                    .id(userId)
                    .email("test@example.com")
                    .role(Role.CUSTOMER)
                    .build();

            when(repository.findById(userId)).thenReturn(Optional.of(user));

            userService.grantAdminRole(userId);

            assertEquals(Role.ADMIN, user.getRole());
        }

        @Test
        void shouldThrowExceptionWhenUserNotFound() {
            UUID userId = UUID.randomUUID();
            when(repository.findById(userId)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> userService.grantAdminRole(userId));
        }
    }

    @Nested
    class GrantOrganizationRole {
        @Test
        void shouldGrantOrganizationRoleSuccessfully() {
            UUID userId = UUID.randomUUID();
            User user = User.builder()
                    .id(userId)
                    .email("test@example.com")
                    .role(Role.CUSTOMER)
                    .build();

            when(repository.findById(userId)).thenReturn(Optional.of(user));

            userService.grantOrganizationRole(userId);

            assertEquals(Role.ORGANIZATION, user.getRole());
        }

        @Test
        void shouldThrowExceptionWhenUserNotFound() {
            UUID userId = UUID.randomUUID();
            when(repository.findById(userId)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> userService.grantOrganizationRole(userId));
        }
    }

    @Nested
    class GetUserProfileByEmail {
        @Test
        void shouldReturnUserProfileSuccessfully() {
            String email = "test@example.com";
            User user = User.builder()
                    .id(UUID.randomUUID())
                    .email(email)
                    .role(Role.CUSTOMER)
                    .build();
            UserDTO expectedDto = new UserDTO(user.getId(), user.getEmail(), user.getRole());

            when(repository.findByEmail(email)).thenReturn(Optional.of(user));

            UserDTO result = userService.getUserProfileByEmail(email);

            assertNotNull(result);
            assertEquals(expectedDto, result);
        }

        @Test
        void shouldThrowExceptionWhenUserNotFound() {
            String email = "notfound@example.com";
            when(repository.findByEmail(email)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> userService.getUserProfileByEmail(email));
        }
    }
}
