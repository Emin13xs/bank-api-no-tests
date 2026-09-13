package com.example.bankapi.controller;

import com.example.bankapi.domain.Transaction;
import com.example.bankapi.dto.TransactionResponse;
import com.example.bankapi.dto.TransferRequest;
import com.example.bankapi.mapper.TransactionMapper;
import com.example.bankapi.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
@Tag(name = "Transfers", description = "Money movement between two accounts")
public class TransferController {

    private final TransferService transferService;
    private final TransactionMapper transactionMapper;

    @Operation(summary = "Transfer funds between two accounts (idempotent)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse transfer(@Valid @RequestBody TransferRequest request) {
        Transaction tx = transferService.transfer(
                request.fromAccountId(), request.toAccountId(), request.amount(), request.idempotencyKey());
        return transactionMapper.toResponse(tx);
    }
}
