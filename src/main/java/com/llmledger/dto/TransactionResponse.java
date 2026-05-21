package com.llmledger.dto;

import java.time.LocalDateTime;

public class TransactionResponse {
    private Long id;
    private Double amount;
    private String description;
    private String type;
    private String category;
    private String ledger;
    private String receipt_url;
    private String ocr_text;
    private LocalDateTime date;
    private Long owner_id;

    public TransactionResponse() {
    }

    public TransactionResponse(Long id, Double amount, String description, String type, String category,
                               String ledger, String receiptUrl, String ocrText, LocalDateTime date, Long ownerId) {
        this.id = id;
        this.amount = amount;
        this.description = description;
        this.type = type;
        this.category = category;
        this.ledger = ledger;
        this.receipt_url = receiptUrl;
        this.ocr_text = ocrText;
        this.date = date;
        this.owner_id = ownerId;
    }

    public Long getId() {
        return id;
    }

    public Double getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public String getType() {
        return type;
    }

    public String getCategory() {
        return category;
    }

    public String getLedger() {
        return ledger;
    }

    public String getReceipt_url() {
        return receipt_url;
    }

    public String getOcr_text() {
        return ocr_text;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public Long getOwner_id() {
        return owner_id;
    }
}
