package com.eretain.auth.controller;

import com.eretain.auth.dto.*;
import com.eretain.auth.service.UserService;
import com.eretain.common.dto.ApiResponse;
import com.eretain.common.dto.PagedResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Lightweight endpoint returning user id → display name map.
     * Accessible to all authenticated users (needed for allocation/timesheet display).
     */
    @GetMapping("/names")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<Long, String>>> getUserNames() {
        Map<Long, String> names = userService.getAllUserNames();
        return ResponseEntity.ok(ApiResponse.success(names));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO')")
    public ResponseEntity<ApiResponse<PagedResponse<UserDTO>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(userService.getAllUsers(page, size)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO')")
    public ResponseEntity<ApiResponse<UserDTO>> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO')")
    public ResponseEntity<ApiResponse<UserDTO>> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.success("User created", userService.createUser(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO')")
    public ResponseEntity<ApiResponse<UserDTO>> updateUser(@PathVariable Long id,
                                                            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.success("User updated", userService.updateUser(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("User deleted", null));
    }

    @PatchMapping("/{id}/enable")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO')")
    public ResponseEntity<ApiResponse<UserDTO>> enableUser(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("User enabled", userService.enableUser(id)));
    }

    @PatchMapping("/{id}/disable")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO')")
    public ResponseEntity<ApiResponse<UserDTO>> disableUser(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("User disabled", userService.disableUser(id)));
    }

    // Access management
    @PostMapping("/{userId}/access")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO')")
    public ResponseEntity<ApiResponse<UserAccessDTO>> grantAccess(@PathVariable Long userId,
                                                                    @RequestBody UserAccessDTO accessDTO) {
        return ResponseEntity.ok(ApiResponse.success("Access granted", userService.grantAccess(userId, accessDTO)));
    }

    @PatchMapping("/{userId}/access/{moduleName}/toggle")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO')")
    public ResponseEntity<ApiResponse<UserAccessDTO>> toggleAccess(@PathVariable Long userId,
                                                                     @PathVariable String moduleName,
                                                                     @RequestParam boolean enabled) {
        return ResponseEntity.ok(ApiResponse.success(userService.toggleAccess(userId, moduleName, enabled)));
    }

    @GetMapping("/{userId}/access")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO')")
    public ResponseEntity<ApiResponse<List<UserAccessDTO>>> getUserAccess(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserAccess(userId)));
    }
}
