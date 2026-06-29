package com.ticket.master.user.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import com.ticket.master.user.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;


@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserRepository repository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        var user = repository.findUserByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));


        List<GrantedAuthority> authorities = new ArrayList<>();

        if (user.getRole() != null) {
            authorities.add(user.getRole());
        }


        return new TMUserDetails(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                authorities
        );
    }
}
