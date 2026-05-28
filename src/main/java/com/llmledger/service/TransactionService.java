package com.llmledger.service;

import com.llmledger.dto.TransactionCreateRequest;
import com.llmledger.entity.FamilyMember;
import com.llmledger.entity.Transaction;
import com.llmledger.entity.User;
import com.llmledger.repository.FamilyMemberRepository;
import com.llmledger.repository.TransactionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final FamilyPermissionService familyPermissionService;
    private final FamilyMemberRepository familyMemberRepository;

    public TransactionService(TransactionRepository transactionRepository,
            FamilyPermissionService familyPermissionService,
            FamilyMemberRepository familyMemberRepository) {
        this.transactionRepository = transactionRepository;
        this.familyPermissionService = familyPermissionService;
        this.familyMemberRepository = familyMemberRepository;
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

        // 如果指定了家庭账本ID，设置家庭ID
        if (request.getFamilyId() != null) {
            // 检查用户是否有权限在家庭账本中创建交易
            if (!familyPermissionService.canCreateTransaction(request.getFamilyId(), owner.getId())) {
                throw new SecurityException("您没有权限在该家庭账本中创建交易");
            }
            tx.setFamilyId(request.getFamilyId());
        }

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

    /**
     * 获取用户在家庭账本中的交易列表
     * 返回所有家庭成员的账单（包括个人账单和明确标记为家庭账本的账单）
     */
    public List<Transaction> listByFamily(Long familyId, User user, String ledger, int skip, int limit) {
        // 检查用户是否有权限查看家庭账本
        if (!familyPermissionService.canViewTransactions(familyId, user.getId())) {
            throw new SecurityException("您没有权限查看该家庭账本");
        }

        // 获取所有家庭成员的ID
        List<FamilyMember> members = familyMemberRepository.findByFamilyId(familyId);
        List<Long> memberIds = members.stream()
                .map(m -> m.getUser().getId())
                .collect(Collectors.toList());

        // 获取所有家庭成员的交易
        List<Transaction> allTransactions = new ArrayList<>();
        for (Long memberId : memberIds) {
            List<Transaction> memberTransactions;
            if (ledger == null || "all".equals(ledger)) {
                memberTransactions = transactionRepository.findByOwnerIdOrderByDateDesc(memberId);
            } else {
                memberTransactions = transactionRepository.findByOwnerIdAndLedgerOrderByDateDesc(memberId, ledger);
            }
            allTransactions.addAll(memberTransactions);
        }

        // 按日期降序排序
        allTransactions.sort((a, b) -> b.getDate().compareTo(a.getDate()));

        int from = Math.min(skip, allTransactions.size());
        int to = Math.min(skip + limit, allTransactions.size());
        return allTransactions.subList(from, to);
    }

    /**
     * 获取用户在家庭账本中的所有交易（用于统计）
     * 返回所有家庭成员的账单
     */
    public List<Transaction> allByFamily(Long familyId, User user) {
        // 检查用户是否有权限查看家庭账本
        if (!familyPermissionService.canViewTransactions(familyId, user.getId())) {
            throw new SecurityException("您没有权限查看该家庭账本");
        }

        // 获取所有家庭成员的ID
        List<FamilyMember> members = familyMemberRepository.findByFamilyId(familyId);
        List<Long> memberIds = members.stream()
                .map(m -> m.getUser().getId())
                .collect(Collectors.toList());

        // 获取所有家庭成员的交易
        List<Transaction> allTransactions = new ArrayList<>();
        for (Long memberId : memberIds) {
            allTransactions.addAll(transactionRepository.findByOwnerIdOrderByDateDesc(memberId));
        }

        // 按日期降序排序
        allTransactions.sort((a, b) -> b.getDate().compareTo(a.getDate()));

        return allTransactions;
    }

    public void delete(User owner, Long id) {
        Optional<Transaction> tx = transactionRepository.findById(id);
        if (tx.isEmpty()) {
            throw new IllegalArgumentException("Transaction not found");
        }

        Transaction transaction = tx.get();

        // 如果是家庭账本的交易，使用家庭权限检查
        if (transaction.getFamilyId() != null) {
            if (!familyPermissionService.canDeleteTransaction(transaction, owner.getId())) {
                throw new SecurityException("您没有权限删除该交易");
            }
        } else {
            // 普通账本，只能删除自己的交易
            if (!transaction.getOwner().getId().equals(owner.getId())) {
                throw new IllegalArgumentException("Transaction not found");
            }
        }

        transactionRepository.delete(transaction);
    }

    public void update(User owner, Long id, TransactionCreateRequest request) {
        Optional<Transaction> txOpt = transactionRepository.findById(id);
        if (txOpt.isEmpty()) {
            throw new IllegalArgumentException("Transaction not found");
        }

        Transaction transaction = txOpt.get();

        // 如果是家庭账本的交易，使用家庭权限检查
        if (transaction.getFamilyId() != null) {
            if (!familyPermissionService.canUpdateTransaction(transaction, owner.getId())) {
                throw new SecurityException("您没有权限修改该交易");
            }
        } else {
            // 普通账本，只能修改自己的交易
            if (!transaction.getOwner().getId().equals(owner.getId())) {
                throw new IllegalArgumentException("Transaction not found");
            }
        }

        // 更新交易信息
        transaction.setAmount(Math.abs(request.getAmount()));
        transaction.setDescription(request.getDescription());
        transaction.setType(request.getType());
        transaction.setCategory(request.getCategory());
        if (request.getLedger() != null && !request.getLedger().isBlank()) {
            transaction.setLedger(request.getLedger());
        }

        transactionRepository.save(transaction);
    }

    public List<Transaction> findByType(User owner, String type) {
        return transactionRepository.findByOwnerIdAndType(owner.getId(), type);
    }

    public List<Transaction> findByTypeAndDateRange(User owner, String type, LocalDateTime startDate,
            LocalDateTime endDate) {
        return transactionRepository.findByOwnerIdAndTypeAndDateBetween(owner.getId(), type, startDate, endDate);
    }

    public List<Transaction> all(User owner) {
        return transactionRepository.findByOwnerIdOrderByDateDesc(owner.getId());
    }

    public List<Transaction> findPotentialDuplicates(User owner, TransactionCreateRequest request) {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        return transactionRepository.findPotentialDuplicates(
                owner.getId(),
                Math.abs(request.getAmount()),
                request.getDescription(),
                thirtyDaysAgo);
    }
}