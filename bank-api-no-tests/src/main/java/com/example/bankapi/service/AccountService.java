package com.example.bankapi.service;

import com.example.bankapi.domain.Account;
import com.example.bankapi.domain.Transaction;
import com.example.bankapi.exception.AccountNotFoundException;
import com.example.bankapi.exception.InsufficientFundsException;
import com.example.bankapi.exception.InvalidTransactionException;
import com.example.bankapi.repository.AccountRepository;
import com.example.bankapi.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.Currency;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public Account openAccount(String ownerName, String currencyCode, BigDecimal openingBalance) {
        Currency currency = parseCurrency(currencyCode);
        String accountNumber = generateAccountNumber();
        Account account = new Account(accountNumber, ownerName, currency, openingBalance);
        Account saved = accountRepository.save(account);
        log.info("Opened account {} for '{}' with opening balance {} {}",
                saved.getAccountNumber(), ownerName, openingBalance, currency.getCurrencyCode());
        return saved;
    }

    @Transactional(readOnly = true)
    public Account getById(UUID accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
    }

    @Transactional
    public Transaction deposit(UUID accountId, BigDecimal amount, String idempotencyKey) {
        return transactionRepository.findByIdempotencyKey(idempotencyKey)
                .orElseGet(() -> {
                    Account account = accountRepository.findByIdForUpdate(accountId)
                            .orElseThrow(() -> new AccountNotFoundException(accountId));
                    assertActive(account);

                    Transaction tx = Transaction.deposit(accountId, amount, account.getCurrency(), idempotencyKey);
                    account.credit(amount);
                    tx.markCompleted();

                    log.info("Deposit {} {} to account {}", amount, account.getCurrency(), account.getAccountNumber());
                    return transactionRepository.save(tx);
                });
    }


    @Transactional
    public Transaction withdraw(UUID accountId, BigDecimal amount, String idempotencyKey) {
        return transactionRepository.findByIdempotencyKey(idempotencyKey)
                .orElseGet(() -> {
                    Account account = accountRepository.findByIdForUpdate(accountId)
                            .orElseThrow(() -> new AccountNotFoundException(accountId));
                    assertActive(account);

                    if (account.getBalance().compareTo(amount) < 0) {
                        throw new InsufficientFundsException(accountId, amount, account.getBalance());
                    }

                    Transaction tx = Transaction.withdrawal(accountId, amount, account.getCurrency(), idempotencyKey);
                    account.debit(amount);
                    tx.markCompleted();

                    log.info("Withdraw {} {} from account {}", amount, account.getCurrency(), account.getAccountNumber());
                    return transactionRepository.save(tx);
                });
    }

    private void assertActive(Account account) {
        if (!account.isActive()) {
            throw new InvalidTransactionException(
                    "Account %s is %s and cannot process transactions"
                            .formatted(account.getAccountNumber(), account.getStatus()));
        }
    }

    private Currency parseCurrency(String code) {
        try {
            return Currency.getInstance(code);
        } catch (IllegalArgumentException ex) {
            throw new InvalidTransactionException("Unknown currency code: " + code);
        }
    }

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no 0/O/1/I ambiguity
    private static final SecureRandom RANDOM = new SecureRandom();

    private String generateAccountNumber() {
        String candidate;
        do {
            StringBuilder sb = new StringBuilder(10);
            for (int i = 0; i < 10; i++) {
                sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
            }
            candidate = "ACC-" + sb;
        } while (accountRepository.existsByAccountNumber(candidate));
        return candidate;
    }
}
