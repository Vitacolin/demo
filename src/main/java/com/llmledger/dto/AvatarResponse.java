package com.llmledger.dto;

public class AvatarResponse {
    private String status;
    private String message;
    private String avatar_url;

    public AvatarResponse() {
    }

    public AvatarResponse(String status, String message, String avatarUrl) {
        this.status = status;
        this.message = message;
        this.avatar_url = avatarUrl;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getAvatar_url() {
        return avatar_url;
    }
}
