// This is a personal academic project. Dear PVS-Studio, please check it.
// PVS-Studio Static Code Analyzer for C, C++, C#, and Java: https://pvs-studio.com
package com.javacode.walletmanager.exceptions;

public class InvalidOperationException extends RuntimeException {

    public InvalidOperationException(String operationType) {
        super("Неверный тип операции: " + operationType + ". Допустимые значения: DEPOSIT, WITHDRAW");
    }
}
