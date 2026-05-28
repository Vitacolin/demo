package com.llmledger.repository;

import com.llmledger.entity.SystemNotice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SystemNoticeRepository extends JpaRepository<SystemNotice, Long> {
    List<SystemNotice> findByIsActiveTrueOrderByCreatedAtDesc();
}