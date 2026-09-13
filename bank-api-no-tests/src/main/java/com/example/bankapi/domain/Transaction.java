package com.example.bankapi.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Immutable ledger entry. One row per money movement (deposit, withdrawal, transfer).
 * For transfers, {@code fromAccountId} and {@code toAccountId} are both set; for
 * deposits/withdrawals only one side applies.
 *
 * <p>{@code idempotencyKey} carries a unique DB constraint so retried client requests
 * (e.g. after a timeout) never produce a duplicate movement of funds.</p>
 */
@Entity
@Table(name = "transactions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_transactions_idempotency_key", columnNames = "idempotency_key")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TransactionStatus status;

    @Column
    private UUID fromAccountId;

    @Column
    private UUID toAccountId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    private String failureReason;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant completedAt;

    public static Transaction transfer(UUID fromAccountId, UUID toAccountId, BigDecimal amount,
                                        String currency, String idempotencyKey) {
        Transaction tx = new Transaction();
        tx.type = TransactionType.TRANSFER;
        tx.status = TransactionStatus.PENDING;
        tx.fromAccountId = fromAccountId;
        tx.toAccountId = toAccountId;
        tx.amount = amount;
        tx.currency = currency;
        tx.idempotencyKey = idempotencyKey;
        return tx;
    }

    public static Transaction deposit(UUID toAccountId, BigDecimal amount, String currency, String idempotencyKey) {
        Transaction tx = new Transaction();
        tx.type = TransactionType.DEPOSIT;
        tx.status = TransactionStatus.PENDING;
        tx.toAccountId = toAccountId;
        tx.amount = amount;
        tx.currency = currency;
        tx.idempotencyKey = idempotencyKey;
        return tx;
    }

    public static Transaction withdrawal(UUID fromAccountId, BigDecimal amount, String currency, String idempotencyKey) {
        Transaction tx = new Transaction();
        tx.type = TransactionType.WITHDRAWAL;
        tx.status = TransactionStatus.PENDING;
        tx.fromAccountId = fromAccountId;
        tx.amount = amount;
        tx.currency = currency;
        tx.idempotencyKey = idempotencyKey;
        return tx;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public void markCompleted() {
        this.status = TransactionStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    public void markFailed(String reason) {
        this.status = TransactionStatus.FAILED;
        this.failureReason = reason;
        this.completedAt = Instant.now();
    }
}
