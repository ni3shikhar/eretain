package com.eretain.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service("securityService")
public class SecurityService {

    /**
     * Check if the currently authenticated user is the owner (their userId matches the given employeeId).
     * Used in @PreAuthorize expressions like: @securityService.isOwner(#employeeId)
     */
    public boolean isOwner(Long employeeId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            return false;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserPrincipal) {
            UserPrincipal userPrincipal = (UserPrincipal) principal;
            return employeeId != null && employeeId.equals(userPrincipal.getUserId());
        }

        return false;
    }
}
