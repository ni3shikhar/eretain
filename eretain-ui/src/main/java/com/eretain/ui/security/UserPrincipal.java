package com.eretain.ui.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class UserPrincipal {
    private Long userId;
    private String username;
    private List<String> roles;

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    public boolean isAdmin() {
        return hasRole("ADMINISTRATOR");
    }

    public boolean isPmo() {
        return hasRole("PMO");
    }

    public boolean isEmployee() {
        return hasRole("EMPLOYEE");
    }
}
