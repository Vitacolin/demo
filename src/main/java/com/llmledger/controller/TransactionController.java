package com.llmledger.controller;

import com.llmledger.dto.DuplicateTransactionResponse;
import com.llmledger.dto.MessageResponse;
import com.llmledger.dto.TransactionCreateRequest;
import com.llmledger.dto.TransactionResponse;
import com.llmledger.entity.Transaction;
import com.llmledger.entity.User;
import com.llmledger.service.DuplicateDetectionService;
import com.llmledger.service.DtoMapper;
import com.llmledger.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

        private final TransactionService transactionService;
        private final DuplicateDetectionService duplicateDetectionService;
        private final DtoMapper mapper;

        public TransactionController(TransactionService transactionService,
                        DuplicateDetectionService duplicateDetectionService,
                        DtoMapper mapper) {
                this.transactionService = transactionService;
                this.duplicateDetectionService = duplicateDetectionService;
                this.mapper = mapper;
        }

        @PostMapping
        public ResponseEntity<?> create(@Valid @RequestBody TransactionCreateRequest request,
                        Authentication authentication) {
                User user = (User) authentication.getPrincipal();

                // 使用智能重复检测
                List<Transaction> duplicates = duplicateDetectionService.detectDuplicatesWithFamily(
                                user.getId(),
                                Math.abs(request.getAmount()),
                                request.getDescription(),
                                request.getFamilyId());

                if (!duplicates.isEmpty()) {
                        List<TransactionResponse> duplicateResponses = duplicates.stream()
                                        .map(mapper::toTransactionResponse)
                                        .toList();
                        String message = duplicateDetectionService.getDetectionMessage(duplicates);
                        return ResponseEntity.ok(new DuplicateTransactionResponse(
                                        true,
                                        message,
                                        duplicateResponses));
                }

                Transaction tx = transactionService.create(request, user);
                return ResponseEntity.ok(mapper.toTransactionResponse(tx));
        }

        @GetMapping("/check-duplicate")
        public ResponseEntity<DuplicateTransactionResponse> checkDuplicate(
                        @RequestParam Double amount,
                        @RequestParam String description,
                        @RequestParam(required = false) Long familyId,
                        Authentication authentication) {
                User user = (User) authentication.getPrincipal();

                // 使用智能重复检测
                List<Transaction> duplicates = duplicateDetectionService.detectDuplicatesWithFamily(
                                user.getId(),
                                Math.abs(amount),
                                description,
                                familyId);

                if (!duplicates.isEmpty()) {
                        List<TransactionResponse> duplicateResponses = duplicates.stream()
                                        .map(mapper::toTransactionResponse)
                                        .toList();
                        String message = duplicateDetectionService.getDetectionMessage(duplicates);
                        return ResponseEntity.ok(new DuplicateTransactionResponse(
                                        true,
                                        message,
                                        duplicateResponses));
                }

                return ResponseEntity.ok(new DuplicateTransactionResponse(
                                false,
                                "未检测到重复账单",
                                List.of()));
        }

        @GetMapping
        public List<TransactionResponse> list(@RequestParam(defaultValue = "all") String ledger,
                        @RequestParam(required = false) Long familyId,
                        @RequestParam(defaultValue = "0") int skip,
                        @RequestParam(defaultValue = "100") int limit,
                        Authentication authentication) {
                User user = (User) authentication.getPrincipal();

                // 如果指定了家庭账本ID，获取家庭账本的交易
                if (familyId != null) {
                        return transactionService.listByFamily(familyId, user, ledger, skip, limit)
                                        .stream()
                                        .map(mapper::toTransactionResponse)
                                        .toList();
                }

                return transactionService.list(user, ledger, skip, limit).stream().map(mapper::toTransactionResponse)
                                .toList();
        }

        @DeleteMapping("/{txId}")
        public MessageResponse delete(@PathVariable Long txId, Authentication authentication) {
                User user = (User) authentication.getPrincipal();
                transactionService.delete(user, txId);
                return new MessageResponse("success", "Deleted transaction " + txId);
        }
}