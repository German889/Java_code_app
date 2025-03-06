package com.javacode.walletmanager.repository;

import com.javacode.walletmanager.model.Transaction;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Transaction> findByWalletId(UUID walletId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Transaction save(Transaction transaction);
}
