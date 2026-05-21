package com.llmledger.dto;

import jakarta.validation.constraints.NotBlank;

public class PasswordChangeRequest {

    @NotBlank
    private String old_password;

    @NotBlank
    private String new_password;

    public String getOld_password() {
        return old_password;
    }

    public void setOld_password(String old_password) {
        this.old_password = old_password;
    }

    public String getNew_password() {
        return new_password;
    }

    public void setNew_password(String new_password) {
        this.new_password = new_password;
    }
}
