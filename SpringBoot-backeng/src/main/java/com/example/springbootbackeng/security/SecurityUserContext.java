package com.example.springbootbackeng.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUserContext {

    private SecurityUserContext() {
    }

    public static AuthenticatedUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedUser currentUser) {
            return currentUser;
        }
        return null;
    }

    public static String getCurrentUserId() {
        AuthenticatedUser currentUser = getCurrentUser();
        return currentUser == null ? null : currentUser.getUserId();
    }

    public static String getCurrentUsername() {
        AuthenticatedUser currentUser = getCurrentUser();
        return currentUser == null ? null : currentUser.getUsername();
    }

    public static String getCurrentRoleCode() {
        AuthenticatedUser currentUser = getCurrentUser();
        return currentUser == null ? null : currentUser.getRoleCode();
    }
}
