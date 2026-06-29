package com.ticket.master.user.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

import org.jspecify.annotations.NonNull;

@Slf4j
@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;

    public JwtFilter(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,@NonNull HttpServletResponse response,@NonNull FilterChain filterChain) throws ServletException, IOException {
        try{
            String jwt = parseJwt(request);

            if(jwt != null && jwtUtils.validateToken(jwt)){
                String username = jwtUtils.getUsernameFromJwtToken(jwt);
                List<GrantedAuthority> authorities = jwtUtils.getAuthoritiesFromToken(jwt);

                UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                        username,
                        "",
                        authorities
                );

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, authorities
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }catch (Exception e){
            log.error("Unable to set user authentication: {}", e.getMessage());
        }
        filterChain.doFilter(request, response);
    }


    private String parseJwt(HttpServletRequest request){
        if (request.getCookies() != null){
            for (Cookie cookie : request.getCookies()){
                if("accessToken".equals(cookie.getName())){
                    return cookie.getValue();
                }
            }
        }
        String headerAuth = request.getHeader("Authorization");
        if(StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")){
            return headerAuth.substring(7);
        }

        return null;
    }
}
