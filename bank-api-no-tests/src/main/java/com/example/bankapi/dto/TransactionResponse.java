package com.example.bankapi.dto;

import com.example.bankapi.domain.TransactionStatus;
import com.example.bankapi.domain.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        TransactionType type,
        TransactionStatus status,
        UUID fromAccountId,
        UUID toAccountId,
        BigDecimal amount,
        String currency,
        String failureReason,
        Instant createdAt,
        Instant completedAt
) {
}
