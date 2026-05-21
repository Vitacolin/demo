package com.llmledger.dto;

import java.time.LocalDateTime;

public class SubscriptionResponse {
    private Long id;
    private String name;
    private Double amount;
    private String cycle;
    private LocalDateTime next_billing_date;
    private String category;
    private Boolean is_active;
    private Long owner_id;

    public SubscriptionResponse() {
    }

    public SubscriptionResponse(Long id, String name, Double amount, String cycle,
                                LocalDateTime nextBillingDate, String category,
                                Boolean isActive, Long ownerId) {
        this.id = id;
        this.name = name;
        this.amount = amount;
        this.cycle = cycle;
        this.next_billing_date = nextBillingDate;
        this.category = category;
        this.is_active = isActive;
        this.owner_id = ownerId;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Double getAmount() {
        return amount;
    }

    public String getCycle() {
        return cycle;
    }

    public LocalDateTime getNext_billing_date() {
        return next_billing_date;
    }

    public String getCategory() {
        return category;
    }

    public Boolean getIs_active() {
        return is_active;
    }

    public Long getOwner_id() {
        return owner_id;
    }
}
