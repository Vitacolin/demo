package com.llmledger.controller;

import com.llmledger.dto.MessageResponse;
import com.llmledger.dto.TransactionCreateRequest;
import com.llmledger.dto.TransactionResponse;
import com.llmledger.entity.Transaction;
import com.llmledger.entity.User;
import com.llmledger.service.DtoMapper;
import com.llmledger.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.Authentication;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final DtoMapper mapper;

    public TransactionController(TransactionService transactionService, DtoMapper mapper) {
        this.transactionService = transactionService;
        this.mapper = mapper;
    }

    @PostMapping
    public TransactionResponse create(@Valid @RequestBody TransactionCreateRequest request,
                                      Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Transaction tx = transactionService.create(request, user);
        return mapper.toTransactionResponse(tx);
    }

    @GetMapping
    public List<TransactionResponse> list(@RequestParam(defaultValue = "all") String ledger,
                                          @RequestParam(defaultValue = "0") int skip,
                                          @RequestParam(defaultValue = "100") int limit,
                                          Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return transactionService.list(user, ledger, skip, limit).stream().map(mapper::toTransactionResponse).toList();
    }

    @DeleteMapping("/{txId}")
    public MessageResponse delete(@PathVariable Long txId, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        transactionService.delete(user, txId);
        return new MessageResponse("success", "Deleted transaction " + txId);
    }
}
