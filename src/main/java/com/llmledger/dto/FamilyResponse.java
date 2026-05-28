package com.llmledger.dto;

import java.time.LocalDateTime;
import java.util.List;

public class FamilyResponse {
    private Long id;
    private String name;
    private LocalDateTime createdAt;
    private Long createdBy;
    private List<MemberResponse> members;
    private Long transactionCount;

    public FamilyResponse() {
    }

    public FamilyResponse(Long id, String name, LocalDateTime createdAt, Long createdBy, List<MemberResponse> members,
            Long transactionCount) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.members = members;
        this.transactionCount = transactionCount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public List<MemberResponse> getMembers() {
        return members;
    }

    public void setMembers(List<MemberResponse> members) {
        this.members = members;
    }

    public Long getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(Long transactionCount) {
        this.transactionCount = transactionCount;
    }

    public static class MemberResponse {
        private Long userId;
        private String username;
        private String role;

        public MemberResponse() {
        }

        public MemberResponse(Long userId, String username, String role) {
            this.userId = userId;
            this.username = username;
            this.role = role;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }
    }
}