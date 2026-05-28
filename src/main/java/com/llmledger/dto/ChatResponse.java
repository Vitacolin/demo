package com.llmledger.dto;

import java.util.List;

public class ChatResponse {
    private String message;
    private List<TransactionCreateRequest> parsed_transactions;

    private boolean hasDuplicates;
    private String duplicateMessage;
    private List<TransactionResponse> potentialDuplicates;

    public ChatResponse() {
    }

    public ChatResponse(String message, List<TransactionCreateRequest> parsedTransactions) {
        this.message = message;
        this.parsed_transactions = parsedTransactions;
        this.hasDuplicates = false;
        this.duplicateMessage = null;
        this.potentialDuplicates = List.of();
    }

    public ChatResponse(String message, List<TransactionCreateRequest> parsedTransactions,
            boolean hasDuplicates, String duplicateMessage, List<TransactionResponse> potentialDuplicates) {
        this.message = message;
        this.parsed_transactions = parsedTransactions;
        this.hasDuplicates = hasDuplicates;
        this.duplicateMessage = duplicateMessage;
        this.potentialDuplicates = potentialDuplicates;
    }

    public String getMessage() {
        return message;
    }

    public List<TransactionCreateRequest> getParsed_transactions() {
        return parsed_transactions;
    }

    public boolean isHasDuplicates() {
        return hasDuplicates;
    }

    public void setHasDuplicates(boolean hasDuplicates) {
        this.hasDuplicates = hasDuplicates;
    }

    public String getDuplicateMessage() {
        return duplicateMessage;
    }

    public void setDuplicateMessage(String duplicateMessage) {
        this.duplicateMessage = duplicateMessage;
    }

    public List<TransactionResponse> getPotentialDuplicates() {
        return potentialDuplicates;
    }

    public void setPotentialDuplicates(List<TransactionResponse> potentialDuplicates) {
        this.potentialDuplicates = potentialDuplicates;
    }
}
