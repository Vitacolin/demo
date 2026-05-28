package com.llmledger.service;

import com.llmledger.dto.SubscriptionResponse;
import com.llmledger.dto.TransactionResponse;
import com.llmledger.dto.UserResponse;
import com.llmledger.entity.Subscription;
import com.llmledger.entity.Transaction;
import com.llmledger.entity.User;
import org.springframework.stereotype.Service;

@Service
public class DtoMapper {

    public UserResponse toUserResponse(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getAvatarUrl(), user.getCreatedAt(),
                user.getRole().name());
    }

    public TransactionResponse toTransactionResponse(Transaction tx) {
        return new TransactionResponse(
                tx.getId(),
                tx.getAmount(),
                tx.getDescription(),
                tx.getType(),
                tx.getCategory(),
                tx.getLedger(),
                tx.getReceiptUrl(),
                tx.getOcrText(),
                tx.getDate(),
                tx.getOwner().getId());
    }

    public SubscriptionResponse toSubscriptionResponse(Subscription sub) {
        return new SubscriptionResponse(
                sub.getId(),
                sub.getName(),
                sub.getAmount(),
                sub.getCycle(),
                sub.getNextBillingDate(),
                sub.getCategory(),
                sub.getIsActive(),
                sub.getOwner().getId());
    }
}
