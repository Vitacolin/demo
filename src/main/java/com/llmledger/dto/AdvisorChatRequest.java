package com.llmledger.dto;

public class AdvisorChatRequest {
    private String question;
    private String persona = "roast";
    private String currentReport;
    private Long familyId;

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getPersona() {
        return persona;
    }

    public void setPersona(String persona) {
        this.persona = persona;
    }

    public String getCurrentReport() {
        return currentReport;
    }

    public void setCurrentReport(String currentReport) {
        this.currentReport = currentReport;
    }

    public Long getFamilyId() {
        return familyId;
    }

    public void setFamilyId(Long familyId) {
        this.familyId = familyId;
    }
}
