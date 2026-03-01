package com.eretain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAccessDTO {
    private Long id;
    private Long userId;
    private String moduleName;
    private boolean canRead;
    private boolean canWrite;
    private boolean canDelete;
    private boolean enabled;
}
