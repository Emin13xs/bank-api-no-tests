package com.example.bankapi.mapper;

import com.example.bankapi.domain.Transaction;
import com.example.bankapi.dto.TransactionResponse;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public TransactionResponse toResponse(Transaction tx) {
        return new TransactionResponse(
                tx.getId(),
                tx.getType(),
                tx.getStatus(),
                tx.getFromAccountId(),
                tx.getToAccountId(),
                tx.getAmount(),
                tx.getCurrency(),
                tx.getFailureReason(),
                tx.getCreatedAt(),
                tx.getCompletedAt()
        );
    }
}
