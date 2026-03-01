package com.eretain.auth.service;

import com.eretain.auth.dto.*;
import com.eretain.auth.entity.User;
import com.eretain.auth.repository.UserRepository;
import com.eretain.common.enums.EmployeeStatus;
import com.eretain.common.enums.Role;
import com.eretain.common.exception.BusinessValidationException;
import com.eretain.common.exception.ResourceNotFoundException;
import com.eretain.common.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessValidationException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessValidationException("Invalid username or password");
        }

        if (user.getStatus() != EmployeeStatus.ACTIVE) {
            throw new BusinessValidationException("User account is not active");
        }

        List<String> roles = user.getRoles().stream()
                .map(Enum::name)
                .collect(Collectors.toList());

        String token = jwtUtil.generateToken(user.getUsername(), user.getId(), roles);

        return LoginResponse.builder()
                .token(token)
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(roles)
                .userId(user.getId())
                .build();
    }

    @Transactional
    public UserDTO register(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessValidationException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessValidationException("Email already exists");
        }

        Set<Role> roles = Set.of(Role.EMPLOYEE);
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            roles = request.getRoles().stream()
                    .map(Role::valueOf)
                    .collect(Collectors.toSet());
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .employeeId(request.getEmployeeId())
                .designation(request.getDesignation())
                .phone(request.getPhone())
                .status(EmployeeStatus.ACTIVE)
                .roles(roles)
                .build();

        User saved = userRepository.save(user);
        return mapToDTO(saved);
    }

    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return mapToDTO(user);
    }

    private UserDTO mapToDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .employeeId(user.getEmployeeId())
                .designation(user.getDesignation())
                .phone(user.getPhone())
                .status(user.getStatus())
                .roles(user.getRoles().stream().map(Enum::name).collect(Collectors.toSet()))
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
