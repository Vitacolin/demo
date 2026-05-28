package com.llmledger.repository;

import com.llmledger.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
        List<Transaction> findByOwnerIdOrderByDateDesc(Long ownerId);

        List<Transaction> findByOwnerIdAndLedgerOrderByDateDesc(Long ownerId, String ledger);

        List<Transaction> findByOwnerIdAndType(Long ownerId, String type);

        @Query("SELECT t FROM Transaction t WHERE t.owner.id = :ownerId AND t.type = :type AND t.date >= :startDate AND t.date < :endDate ORDER BY t.date DESC")
        List<Transaction> findByOwnerIdAndTypeAndDateBetween(@Param("ownerId") Long ownerId, @Param("type") String type,
                        @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

        @Query("SELECT t FROM Transaction t WHERE t.owner.id = :ownerId AND ABS(t.amount - :amount) < 0.01 AND t.description LIKE %:description% AND t.date >= :startDate ORDER BY t.date DESC")
        List<Transaction> findPotentialDuplicates(
                        @Param("ownerId") Long ownerId,
                        @Param("amount") Double amount,
                        @Param("description") String description,
                        @Param("startDate") LocalDateTime startDate);

        // 家庭账本相关查询
        List<Transaction> findByFamilyIdOrderByDateDesc(Long familyId);

        List<Transaction> findByFamilyIdAndLedgerOrderByDateDesc(Long familyId, String ledger);

        @Query("SELECT COUNT(t) FROM Transaction t WHERE t.familyId = :familyId")
        long countByFamilyId(@Param("familyId") Long familyId);

        @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.familyId = :familyId AND t.type = 'expense'")
        Double sumExpenseByFamilyId(@Param("familyId") Long familyId);

        @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.familyId = :familyId AND t.type = 'income'")
        Double sumIncomeByFamilyId(@Param("familyId") Long familyId);
}