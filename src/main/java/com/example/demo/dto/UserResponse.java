package com.example.demo.dto;

public class UserResponse {
    private String username;
    private boolean verified;

    public UserResponse(String username, boolean verified) {
        this.username = username;
        this.verified = verified;
    }

    // Getters only
    public String getUsername() { return username; }
    public boolean isVerified() { return verified; }
}
