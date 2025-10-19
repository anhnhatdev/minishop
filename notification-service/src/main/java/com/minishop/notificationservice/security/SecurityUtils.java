package com.minishop.notificationservice.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static UserPrincipal getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal;
        }
        return null;
    }

    public static UUID getCurrentUserId() {
        UserPrincipal principal = getCurrentUser();
        return principal != null ? principal.getId() : null;
    }

    public static String getCurrentUserRole() {
        UserPrincipal principal = getCurrentUser();
        return principal != null ? principal.getRole() : null;
    }

    public static boolean isAdmin() {
        String role = getCurrentUserRole();
        return "ADMIN".equalsIgnoreCase(role);
    }
}
