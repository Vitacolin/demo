package com.llmledger.dto;

public class TokenResponse {
    private String access_token;
    private String token_type = "bearer";

    public TokenResponse() {
    }

    public TokenResponse(String accessToken) {
        this.access_token = accessToken;
    }

    public String getAccess_token() {
        return access_token;
    }

    public String getToken_type() {
        return token_type;
    }
}
