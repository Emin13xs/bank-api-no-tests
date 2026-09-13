package com.example.bankapi.repository;

import com.example.bankapi.domain.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    Optional<Account> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);

    /**
     * Row-level lock (SELECT ... FOR UPDATE) used by transfer/withdraw/deposit
     * operations. Callers MUST always acquire locks for multiple accounts in a
     * consistent order (e.g. by id) to avoid deadlocks — see
     * {@code TransferService#lockAccountsInOrder}.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id = :id")
    Optional<Account> findByIdForUpdate(UUID id);
}
