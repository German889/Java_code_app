// This is a personal academic project. Dear PVS-Studio, please check it.
// PVS-Studio Static Code Analyzer for C, C++, C#, and Java: https://pvs-studio.com
package com.javacode.walletmanager.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.UUID;

public class WalletResponse {

    @JsonProperty("walletId")
    private UUID walletId;

    @JsonProperty("balance")
    private BigDecimal balance;

    public WalletResponse(UUID walletId, BigDecimal balance) {
        this.walletId = walletId;
        this.balance = balance;
    }

    public UUID getWalletId() {
        return walletId;
    }

    public BigDecimal getBalance() {
        return balance;
    }
}
