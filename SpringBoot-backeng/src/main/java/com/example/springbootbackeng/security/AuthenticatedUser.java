package com.example.springbootbackeng.security;

public class AuthenticatedUser {

    private final String userId;
    private final String username;
    private final String roleCode;

    public AuthenticatedUser(String userId, String username, String roleCode) {
        this.userId = userId;
        this.username = username;
        this.roleCode = roleCode;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getRoleCode() {
        return roleCode;
    }
}
