package com.llmledger.dto;

import java.util.List;

public class DuplicateTransactionResponse {
    private boolean hasDuplicates;
    private String message;
    private List<TransactionResponse> potentialDuplicates;

    public DuplicateTransactionResponse() {}

    public DuplicateTransactionResponse(boolean hasDuplicates, String message, List<TransactionResponse> potentialDuplicates) {
        this.hasDuplicates = hasDuplicates;
        this.message = message;
        this.potentialDuplicates = potentialDuplicates;
    }

    public boolean isHasDuplicates() {
        return hasDuplicates;
    }

    public void setHasDuplicates(boolean hasDuplicates) {
        this.hasDuplicates = hasDuplicates;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<TransactionResponse> getPotentialDuplicates() {
        return potentialDuplicates;
    }

    public void setPotentialDuplicates(List<TransactionResponse> potentialDuplicates) {
        this.potentialDuplicates = potentialDuplicates;
    }
}