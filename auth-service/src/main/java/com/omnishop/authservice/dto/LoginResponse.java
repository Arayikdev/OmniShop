package com.omnishop.authservice.dto;

import java.util.UUID;

public class LoginResponse {

    private String token;
    private UUID userId;
    private String role;
    private long expiresIn = 86400;

    public LoginResponse() {}

    public LoginResponse(String token, UUID userId, String role, long expiresIn) {
        this.token = token;
        this.userId = userId;
        this.role = role;
        this.expiresIn = expiresIn;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public long getExpiresIn() { return expiresIn; }
    public void setExpiresIn(long expiresIn) { this.expiresIn = expiresIn; }
}
