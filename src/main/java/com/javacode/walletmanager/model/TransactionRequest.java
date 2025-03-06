package com.javacode.walletmanager.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.UUID;

public class TransactionRequest {

    @JsonProperty("requestId")
    private UUID requestId;

    @JsonProperty("walletId")
    private UUID walletId;

    @JsonProperty("operationType")
    private String operationType;

    @JsonProperty("amount")
    private BigDecimal amount;

    public TransactionRequest() {
    }

    public TransactionRequest(UUID requestId, UUID walletId, String operationType, BigDecimal amount) {
        this.requestId = requestId;
        this.walletId = walletId;
        this.operationType = operationType;
        this.amount = amount;
    }

    public UUID getRequestId() {
        return requestId;
    }

    public void setRequestId(UUID requestId) {
        this.requestId = requestId;
    }

    public UUID getWalletId() {
        return walletId;
    }

    public void setWalletId(UUID walletId) {
        this.walletId = walletId;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}