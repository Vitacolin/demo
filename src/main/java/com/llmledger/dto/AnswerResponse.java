package com.llmledger.dto;

public class AnswerResponse {
    private String answer;
    private boolean isReportUpdate;

    public AnswerResponse() {
    }

    public AnswerResponse(String answer, boolean isReportUpdate) {
        this.answer = answer;
        this.isReportUpdate = isReportUpdate;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public boolean getIsReportUpdate() {
        return isReportUpdate;
    }

    public void setIsReportUpdate(boolean isReportUpdate) {
        this.isReportUpdate = isReportUpdate;
    }
}
