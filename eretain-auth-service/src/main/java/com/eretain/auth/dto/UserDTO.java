package com.eretain.auth.dto;

import com.eretain.common.enums.EmployeeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String employeeId;
    private String designation;
    private String phone;
    private EmployeeStatus status;
    private Set<String> roles;
    private List<UserAccessDTO> accesses;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
