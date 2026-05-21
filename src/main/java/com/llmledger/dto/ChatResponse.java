package com.llmledger.dto;

import java.util.List;

public class ChatResponse {
    private String message;
    private List<TransactionCreateRequest> parsed_transactions;

    public ChatResponse() {
    }

    public ChatResponse(String message, List<TransactionCreateRequest> parsedTransactions) {
        this.message = message;
        this.parsed_transactions = parsedTransactions;
    }

    public String getMessage() {
        return message;
    }

    public List<TransactionCreateRequest> getParsed_transactions() {
        return parsed_transactions;
    }
}
