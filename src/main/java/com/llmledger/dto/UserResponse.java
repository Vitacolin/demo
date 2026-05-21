package com.llmledger.dto;

import java.time.LocalDateTime;

public class UserResponse {
    private Long id;
    private String username;
    private String avatar_url;
    private LocalDateTime created_at;

    public UserResponse() {
    }

    public UserResponse(Long id, String username, String avatarUrl, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.avatar_url = avatarUrl;
        this.created_at = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getAvatar_url() {
        return avatar_url;
    }

    public LocalDateTime getCreated_at() {
        return created_at;
    }
}
