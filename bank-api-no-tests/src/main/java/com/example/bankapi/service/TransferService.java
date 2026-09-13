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
import java.util.UUID;

/**
 * Moves funds between two accounts.
 *
 * <p><b>Concurrency strategy:</b> both accounts are locked with
 * {@code SELECT ... FOR UPDATE} (see {@link AccountRepository#findByIdForUpdate}),
 * always acquired in ascending order of account id. This total ordering is what
 * prevents the classic transfer deadlock: two concurrent transfers in opposite
 * directions (A→B and B→A) would otherwise each hold one lock and wait forever
 * for the other.</p>
 *
 * <p><b>Idempotency:</b> the caller supplies an {@code idempotencyKey} (e.g. a
 * client-generated UUID per user action). If a transaction with that key already
 * exists we return it as-is instead of moving money again — this makes retries
 * after network timeouts safe.</p>
 */
@Service
@RequiredArgsConstructor
public class TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public Transaction transfer(UUID fromAccountId, UUID toAccountId, BigDecimal amount, String idempotencyKey) {
        if (fromAccountId.equals(toAccountId)) {
            throw new InvalidTransactionException("fromAccountId and toAccountId must differ");
        }

        return transactionRepository.findByIdempotencyKey(idempotencyKey)
                .orElseGet(() -> executeTransfer(fromAccountId, toAccountId, amount, idempotencyKey));
    }

    private Transaction executeTransfer(UUID fromAccountId, UUID toAccountId, BigDecimal amount, String idempotencyKey) {
        Account[] locked = lockAccountsInOrder(fromAccountId, toAccountId);
        Account from = locked[0].getId().equals(fromAccountId) ? locked[0] : locked[1];
        Account to = locked[0].getId().equals(toAccountId) ? locked[0] : locked[1];

        assertActive(from);
        assertActive(to);
        assertSameCurrency(from, to);

        Transaction tx = Transaction.transfer(fromAccountId, toAccountId, amount, from.getCurrency(), idempotencyKey);

        if (from.getBalance().compareTo(amount) < 0) {
            tx.markFailed("Insufficient funds");
            transactionRepository.save(tx);
            throw new InsufficientFundsException(fromAccountId, amount, from.getBalance());
        }

        from.debit(amount);
        to.credit(amount);
        tx.markCompleted();

        log.info("Transferred {} {} from {} to {}", amount, from.getCurrency(),
                from.getAccountNumber(), to.getAccountNumber());

        return transactionRepository.save(tx);
    }

    /** Acquires row locks for both accounts in a fixed order (by UUID) to avoid deadlocks. */
    private Account[] lockAccountsInOrder(UUID idA, UUID idB) {
        UUID first = idA.compareTo(idB) <= 0 ? idA : idB;
        UUID second = idA.compareTo(idB) <= 0 ? idB : idA;

        Account firstAccount = accountRepository.findByIdForUpdate(first)
                .orElseThrow(() -> new AccountNotFoundException(first));
        Account secondAccount = accountRepository.findByIdForUpdate(second)
                .orElseThrow(() -> new AccountNotFoundException(second));

        return new Account[]{firstAccount, secondAccount};
    }

    private void assertActive(Account account) {
        if (!account.isActive()) {
            throw new InvalidTransactionException(
                    "Account %s is %s and cannot process transactions"
                            .formatted(account.getAccountNumber(), account.getStatus()));
        }
    }

    private void assertSameCurrency(Account from, Account to) {
        if (!from.getCurrency().equals(to.getCurrency())) {
            throw new InvalidTransactionException(
                    "Cross-currency transfers are not supported (%s -> %s)"
                            .formatted(from.getCurrency(), to.getCurrency()));
        }
    }
}
