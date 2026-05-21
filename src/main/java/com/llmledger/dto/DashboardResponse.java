package com.llmledger.dto;

import java.util.Map;

public class DashboardResponse {
    private Double total_expense;
    private Double total_income;
    private Double balance;
    private Integer health_score;
    private Map<String, Object> sankey;

    public DashboardResponse() {
    }

    public DashboardResponse(Double totalExpense, Double totalIncome, Double balance, Integer healthScore, Map<String, Object> sankey) {
        this.total_expense = totalExpense;
        this.total_income = totalIncome;
        this.balance = balance;
        this.health_score = healthScore;
        this.sankey = sankey;
    }

    public Double getTotal_expense() {
        return total_expense;
    }

    public Double getTotal_income() {
        return total_income;
    }

    public Double getBalance() {
        return balance;
    }

    public Integer getHealth_score() {
        return health_score;
    }

    public Map<String, Object> getSankey() {
        return sankey;
    }
}
