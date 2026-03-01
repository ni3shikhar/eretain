package com.eretain.auth.controller;

import com.eretain.auth.dto.*;
import com.eretain.auth.service.AuthService;
import com.eretain.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Login successful", authService.login(request)));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserDTO>> register(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Registration successful", authService.register(request)));
    }

    @GetMapping("/me/{id}")
    public ResponseEntity<ApiResponse<UserDTO>> getCurrentUser(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(authService.getUserById(id)));
    }
}
