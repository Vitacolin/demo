package com.llmledger.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class TransactionCreateRequest {

    @NotNull
    private Double amount;

    @NotBlank
    private String description;

    @NotBlank
    private String type;

    @NotBlank
    private String category;

    private String ledger = "日常账本";

    private String receipt_url;

    private String ocr_text;

    private Long familyId;

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getLedger() {
        return ledger;
    }

    public void setLedger(String ledger) {
        this.ledger = ledger;
    }

    public String getReceipt_url() {
        return receipt_url;
    }

    public void setReceipt_url(String receipt_url) {
        this.receipt_url = receipt_url;
    }

    public String getOcr_text() {
        return ocr_text;
    }

    public void setOcr_text(String ocr_text) {
        this.ocr_text = ocr_text;
    }

    public Long getFamilyId() {
        return familyId;
    }

    public void setFamilyId(Long familyId) {
        this.familyId = familyId;
    }
}
