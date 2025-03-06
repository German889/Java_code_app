// This is a personal academic project. Dear PVS-Studio, please check it.
// PVS-Studio Static Code Analyzer for C, C++, C#, and Java: https://pvs-studio.com
package com.javacode.walletmanager.exceptions;

import java.math.BigDecimal;

public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(String message) {
        super(message);
    }

    public InsufficientFundsException(BigDecimal balance, BigDecimal amount) {
        super("Недостаточно средств на кошельке. Текущий баланс: " + balance + ", запрошено: " + amount);
    }
}