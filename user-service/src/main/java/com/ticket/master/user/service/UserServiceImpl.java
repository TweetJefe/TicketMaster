package com.ticket.master.user.service;

import com.ticket.master.user.dto.*;
import com.ticket.master.user.exception.UserAlreadyExists;
import jakarta.persistence.EntityNotFoundException;
import com.ticket.master.user.kafka.UserEventProducer;
import com.ticket.master.user.mapper.UserMapper;
import com.ticket.master.user.model.Role;
import com.ticket.master.user.model.User;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ticket.master.user.repository.UserRepository;
import com.ticket.master.user.security.JwtUtils;

import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final UserMapper mapper;
    private final AuthenticationManager authenticationManager;
    private final UserEventProducer userEventProducer;

    public UserServiceImpl(UserRepository repository, PasswordEncoder passwordEncoder, JwtUtils jwtUtils, UserMapper mapper, AuthenticationManager authenticationManager, UserEventProducer userEventProducer, KafkaTemplate<String, Object> kafkaTemplate) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.mapper = mapper;
        this.authenticationManager = authenticationManager;
        this.userEventProducer = userEventProducer;
    }

    @Override
    public UserDTO register(UserRegisterRequest request) {
        if (repository.existsByEmail(request.email())) {
            throw new UserAlreadyExists("User already exists");
        }
        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.CUSTOMER)
                .build();
        User savedUser = repository.save(user);

        UserRegisteredEvent event = UserRegisteredEvent.builder()
                .email(savedUser.getEmail())
                .role(savedUser.getRole().name())
                .build();

        userEventProducer.sendRegistrationEvent(event);
        return mapper.toDto(savedUser);
    }

    @Override
    public AuthResponse login(UserLoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        String token = jwtUtils.generateToken(userDetails);

        return new AuthResponse(token);
    }

    @Override
    @Transactional
    public void grantAdminRole(UUID id) {
        User user = repository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("User not found"));
        user.setRole(Role.ADMIN);
    }

    @Override
    @Transactional
    public void grantOrganizationRole(UUID id) {
        User user = repository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("User not found"));
        user.setRole(Role.ORGANIZATION);
    }

    @Override
    public UserDTO getUserProfileByEmail(String email) {
        User user = repository.findByEmail(email).orElseThrow(
                () -> new EntityNotFoundException("User not found"));
        return mapper.toDto(user);
    }
}
