package com.ticket.master.user.config;

import lombok.RequiredArgsConstructor;
import com.ticket.master.user.model.Role;
import com.ticket.master.user.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import com.ticket.master.user.repository.UserRepository;

@Component
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.default.email}")
    private String adminEmail;

    @Value("${admin.default.password}")
    private String adminPassword;

    @Override
    public void run (String... args) throws Exception{
        if (!repository.existsByEmail(adminEmail)){
            User admin = User.builder()
                    .email(adminEmail)
                    .password(passwordEncoder.encode(adminPassword))
                    .role(Role.ADMIN)
                    .build();

            repository.save(admin);
        }
    }
}
