package com.llmledger.dto;

import java.time.OffsetDateTime;

public class SubscriptionCreateRequest {
    private String name;
    private Double amount;
    private String cycle;
    private OffsetDateTime next_billing_date;
    private String category;
    private Boolean is_active = true;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getCycle() {
        return cycle;
    }

    public void setCycle(String cycle) {
        this.cycle = cycle;
    }

    public OffsetDateTime getNext_billing_date() {
        return next_billing_date;
    }

    public void setNext_billing_date(OffsetDateTime next_billing_date) {
        this.next_billing_date = next_billing_date;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Boolean getIs_active() {
        return is_active;
    }

    public void setIs_active(Boolean is_active) {
        this.is_active = is_active;
    }
}
