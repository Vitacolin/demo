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
    List<Transaction> findByOwnerIdAndTypeAndDateBetween(@Param("ownerId") Long ownerId, @Param("type") String type, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}
