package com.javacode.walletmanager.service;

import com.javacode.walletmanager.model.Transaction;
import com.javacode.walletmanager.model.Wallet;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface WalletService {
    Wallet createWallet(BigDecimal initialBalance);

    Wallet getWalletById(UUID walletId);

    void processTransaction(UUID walletId, String operationType, BigDecimal amount);

    List<Transaction> getTransactionsByWalletId(UUID walletId);
}
