package com.llmledger.service;

import com.llmledger.dto.TransactionCreateRequest;
import com.llmledger.entity.Transaction;
import com.llmledger.entity.User;
import com.llmledger.repository.TransactionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public Transaction create(TransactionCreateRequest request, User owner) {
        Transaction tx = new Transaction();
        tx.setAmount(Math.abs(request.getAmount()));
        tx.setDescription(request.getDescription());
        tx.setType(request.getType());
        tx.setCategory(request.getCategory());
        tx.setLedger(request.getLedger() == null || request.getLedger().isBlank() ? "日常账本" : request.getLedger());
        tx.setReceiptUrl(request.getReceipt_url());
        tx.setOcrText(request.getOcr_text());
        tx.setOwner(owner);
        return transactionRepository.save(tx);
    }

    public List<Transaction> list(User owner, String ledger, int skip, int limit) {
        List<Transaction> list;
        if (ledger == null || "all".equals(ledger)) {
            list = transactionRepository.findByOwnerIdOrderByDateDesc(owner.getId());
        } else {
            list = transactionRepository.findByOwnerIdAndLedgerOrderByDateDesc(owner.getId(), ledger);
        }
        int from = Math.min(skip, list.size());
        int to = Math.min(skip + limit, list.size());
        return list.subList(from, to);
    }

    public void delete(User owner, Long id) {
        Optional<Transaction> tx = transactionRepository.findById(id);
        if (tx.isEmpty() || !tx.get().getOwner().getId().equals(owner.getId())) {
            throw new IllegalArgumentException("Transaction not found");
        }
        transactionRepository.delete(tx.get());
    }

    public List<Transaction> findByType(User owner, String type) {
        return transactionRepository.findByOwnerIdAndType(owner.getId(), type);
    }

    public List<Transaction> findByTypeAndDateRange(User owner, String type, LocalDateTime startDate, LocalDateTime endDate) {
        return transactionRepository.findByOwnerIdAndTypeAndDateBetween(owner.getId(), type, startDate, endDate);
    }

    public List<Transaction> all(User owner) {
        return transactionRepository.findByOwnerIdOrderByDateDesc(owner.getId());
    }
}
