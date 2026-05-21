package com.llmledger.dto;

import java.util.List;
import java.util.Map;

public class AssetsResponse {
    private Double net_worth;
    private List<Map<String, Object>> assets;
    private List<Map<String, Object>> debts;

    public AssetsResponse() {
    }

    public AssetsResponse(Double netWorth, List<Map<String, Object>> assets, List<Map<String, Object>> debts) {
        this.net_worth = netWorth;
        this.assets = assets;
        this.debts = debts;
    }

    public Double getNet_worth() {
        return net_worth;
    }

    public List<Map<String, Object>> getAssets() {
        return assets;
    }

    public List<Map<String, Object>> getDebts() {
        return debts;
    }
}
