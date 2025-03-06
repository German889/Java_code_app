// This is a personal academic project. Dear PVS-Studio, please check it.
// PVS-Studio Static Code Analyzer for C, C++, C#, and Java: https://pvs-studio.com
package com.javacode.walletmanager.service;

import com.javacode.walletmanager.exceptions.InsufficientFundsException;
import com.javacode.walletmanager.exceptions.InvalidOperationException;
import com.javacode.walletmanager.exceptions.WalletNotFoundException;
import com.javacode.walletmanager.model.Transaction;
import com.javacode.walletmanager.model.Wallet;
import com.javacode.walletmanager.repository.TransactionRepository;
import com.javacode.walletmanager.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    @Autowired
    public WalletServiceImpl(WalletRepository walletRepository, TransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional
    public Wallet createWallet(BigDecimal initialBalance) {
        Wallet wallet = new Wallet();
        wallet.setBalance(initialBalance);
        return walletRepository.save(wallet);
    }

    @Override
    @Transactional()
    public Wallet getWalletById(UUID walletId) {
        return walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException("кошелёк не найден"));
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void processTransaction(UUID walletId, String operationType, BigDecimal amount) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException("кошелёк не найден"));

        Transaction transaction = new Transaction();
        transaction.setOperationType(operationType);
        transaction.setAmount(amount);
        transaction.setWallet(wallet);

        switch (operationType.toUpperCase()) {
            case "DEPOSIT" -> wallet.setBalance(wallet.getBalance().add(amount));
            case "WITHDRAW" -> {
                if (wallet.getBalance().compareTo(amount) >= 0) {
                    wallet.setBalance(wallet.getBalance().subtract(amount));
                } else {
                    throw new InsufficientFundsException("недостаточно средств");
                }
            }
            default -> throw new InvalidOperationException("неверный тип операции");
        }

        walletRepository.save(wallet);
        transactionRepository.save(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsByWalletId(UUID walletId) {
        return transactionRepository.findByWalletId(walletId);
    }
}
