package com.ticket.master.user.model;

import org.springframework.security.core.GrantedAuthority;

public enum Role implements GrantedAuthority {
    ADMIN, CUSTOMER, ORGANIZATION;

    @Override
    public String getAuthority() {
        return name();
    }
}
