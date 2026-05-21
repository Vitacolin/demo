package com.llmledger.dto;

public class MessageResponse {
    private String status;
    private String message;

    public MessageResponse() {
    }

    public MessageResponse(String status, String message) {
        this.status = status;
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
