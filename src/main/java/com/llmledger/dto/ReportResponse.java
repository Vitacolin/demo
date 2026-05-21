package com.llmledger.dto;

public class ReportResponse {
    private String report;

    public ReportResponse() {
    }

    public ReportResponse(String report) {
        this.report = report;
    }

    public String getReport() {
        return report;
    }
}
