package com.eretain.auth.service;

import com.eretain.auth.dto.*;
import com.eretain.auth.entity.User;
import com.eretain.auth.entity.UserAccess;
import com.eretain.auth.repository.UserAccessRepository;
import com.eretain.auth.repository.UserRepository;
import com.eretain.common.dto.PagedResponse;
import com.eretain.common.enums.EmployeeStatus;
import com.eretain.common.enums.Role;
import com.eretain.common.exception.BusinessValidationException;
import com.eretain.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserAccessRepository userAccessRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Returns a map of userId → display name for all active users.
     */
    public Map<Long, String> getAllUserNames() {
        List<User> users = userRepository.findAll();
        Map<Long, String> names = new HashMap<>();
        for (User u : users) {
            String displayName = u.getFirstName() != null
                    ? u.getFirstName() + " " + u.getLastName()
                    : u.getUsername();
            names.put(u.getId(), displayName);
        }
        return names;
    }

    public PagedResponse<UserDTO> getAllUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("firstName").ascending());
        Page<User> users = userRepository.findByActiveTrue(pageable);
        return mapToPagedResponse(users);
    }

    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return mapToDTO(user);
    }

    @Transactional
    public UserDTO createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessValidationException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessValidationException("Email already exists");
        }
        if (request.getEmployeeId() != null && userRepository.existsByEmployeeId(request.getEmployeeId())) {
            throw new BusinessValidationException("Employee ID already exists");
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

        return mapToDTO(userRepository.save(user));
    }

    @Transactional
    public UserDTO updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getDesignation() != null) user.setDesignation(request.getDesignation());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getRoles() != null) {
            Set<Role> roles = request.getRoles().stream()
                    .map(Role::valueOf)
                    .collect(Collectors.toSet());
            user.setRoles(roles);
        }

        return mapToDTO(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        user.setActive(false);
        user.setStatus(EmployeeStatus.TERMINATED);
        userRepository.save(user);
    }

    @Transactional
    public UserDTO enableUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        user.setActive(true);
        user.setStatus(EmployeeStatus.ACTIVE);
        return mapToDTO(userRepository.save(user));
    }

    @Transactional
    public UserDTO disableUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        user.setActive(false);
        user.setStatus(EmployeeStatus.INACTIVE);
        return mapToDTO(userRepository.save(user));
    }

    // Access management
    @Transactional
    public UserAccessDTO grantAccess(Long userId, UserAccessDTO accessDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        UserAccess access = userAccessRepository.findByUserIdAndModuleName(userId, accessDTO.getModuleName())
                .orElse(new UserAccess());

        access.setUser(user);
        access.setModuleName(accessDTO.getModuleName());
        access.setCanRead(accessDTO.isCanRead());
        access.setCanWrite(accessDTO.isCanWrite());
        access.setCanDelete(accessDTO.isCanDelete());
        access.setEnabled(accessDTO.isEnabled());

        UserAccess saved = userAccessRepository.save(access);
        return mapAccessToDTO(saved);
    }

    @Transactional
    public UserAccessDTO toggleAccess(Long userId, String moduleName, boolean enabled) {
        UserAccess access = userAccessRepository.findByUserIdAndModuleName(userId, moduleName)
                .orElseThrow(() -> new ResourceNotFoundException("UserAccess", "module", moduleName));
        access.setEnabled(enabled);
        return mapAccessToDTO(userAccessRepository.save(access));
    }

    public List<UserAccessDTO> getUserAccess(Long userId) {
        return userAccessRepository.findByUserId(userId).stream()
                .map(this::mapAccessToDTO)
                .collect(Collectors.toList());
    }

    private UserDTO mapToDTO(User user) {
        List<UserAccessDTO> accesses = user.getUserAccesses() != null
                ? user.getUserAccesses().stream().map(this::mapAccessToDTO).collect(Collectors.toList())
                : null;

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
                .accesses(accesses)
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private UserAccessDTO mapAccessToDTO(UserAccess access) {
        return UserAccessDTO.builder()
                .id(access.getId())
                .userId(access.getUser().getId())
                .moduleName(access.getModuleName())
                .canRead(access.isCanRead())
                .canWrite(access.isCanWrite())
                .canDelete(access.isCanDelete())
                .enabled(access.isEnabled())
                .build();
    }

    private PagedResponse<UserDTO> mapToPagedResponse(Page<User> page) {
        return PagedResponse.<UserDTO>builder()
                .content(page.getContent().stream().map(this::mapToDTO).collect(Collectors.toList()))
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
