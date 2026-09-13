package com.example.bankapi.controller;

import com.example.bankapi.domain.Account;
import com.example.bankapi.domain.Transaction;
import com.example.bankapi.dto.*;
import com.example.bankapi.mapper.AccountMapper;
import com.example.bankapi.mapper.TransactionMapper;
import com.example.bankapi.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Tag(name = "Accounts", description = "Account lifecycle and single-account money movements")
public class AccountController {

    private final AccountService accountService;
    private final AccountMapper accountMapper;
    private final TransactionMapper transactionMapper;

    @Operation(summary = "Open a new account")
    @PostMapping
    public ResponseEntity<AccountResponse> openAccount(@Valid @RequestBody CreateAccountRequest request) {
        Account account = accountService.openAccount(request.ownerName(), request.currency(), request.openingBalance());
        AccountResponse body = accountMapper.toResponse(account);
        return ResponseEntity.created(URI.create("/api/v1/accounts/" + account.getId())).body(body);
    }

    @Operation(summary = "Get account details and current balance")
    @GetMapping("/{accountId}")
    public AccountResponse getAccount(@PathVariable UUID accountId) {
        return accountMapper.toResponse(accountService.getById(accountId));
    }

    @Operation(summary = "Deposit funds into an account (idempotent)")
    @PostMapping("/{accountId}/deposits")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse deposit(@PathVariable UUID accountId, @Valid @RequestBody DepositRequest request) {
        Transaction tx = accountService.deposit(accountId, request.amount(), request.idempotencyKey());
        return transactionMapper.toResponse(tx);
    }

    @Operation(summary = "Withdraw funds from an account (idempotent)")
    @PostMapping("/{accountId}/withdrawals")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse withdraw(@PathVariable UUID accountId, @Valid @RequestBody WithdrawRequest request) {
        Transaction tx = accountService.withdraw(accountId, request.amount(), request.idempotencyKey());
        return transactionMapper.toResponse(tx);
    }
}
