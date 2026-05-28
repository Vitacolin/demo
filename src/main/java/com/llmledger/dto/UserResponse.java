package com.llmledger.dto;

import java.time.LocalDateTime;

public class UserResponse {
    private Long id;
    private String username;
    private String avatarUrl;
    private LocalDateTime createdAt;
    private String role;

    public UserResponse() {
    }

    public UserResponse(Long id, String username, String avatarUrl, LocalDateTime createdAt, String role) {
        this.id = id;
        this.username = username;
        this.avatarUrl = avatarUrl;
        this.createdAt = createdAt;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getRole() {
        return role;
    }
}
