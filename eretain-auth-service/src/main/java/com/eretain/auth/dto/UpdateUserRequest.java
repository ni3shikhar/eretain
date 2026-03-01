package com.eretain.auth.dto;

import jakarta.validation.constraints.Email;
import lombok.Data;

import java.util.Set;

@Data
public class UpdateUserRequest {
    @Email(message = "Invalid email format")
    private String email;
    private String firstName;
    private String lastName;
    private String designation;
    private String phone;
    private Set<String> roles;
}
