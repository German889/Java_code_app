package com.javacode.walletmanager.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javacode.walletmanager.exceptions.WalletNotFoundException;
import com.javacode.walletmanager.model.ResponseRequest;
import com.javacode.walletmanager.model.TransactionRequest;
import com.javacode.walletmanager.model.Wallet;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class TransactionConsumer {

    private final WalletService walletService;
    private final RabbitTemplate rabbitTemplate;
    private static final String RESPONSE_QUEUE_NAME = "responseQueue";

    @Autowired
    public TransactionConsumer(WalletService walletService, RabbitTemplate rabbitTemplate) {
        this.walletService = walletService;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = "walletQueue")
    public void handleTransaction(TransactionRequest request) {
        ResponseRequest responseRequest = new ResponseRequest();
        try {
            if ("BALANCE".equals(request.getOperationType())) {
                Wallet wallet = walletService.getWalletById(request.getWalletId());
                ObjectMapper objectMapper = new ObjectMapper();
                String responseBody = objectMapper.writeValueAsString(Map.of(
                        "walletId", wallet.getId(),
                        "balance", wallet.getBalance()
                ));

                responseRequest = new ResponseRequest(request.getRequestId(), responseBody, null);
                rabbitTemplate.convertAndSend(RESPONSE_QUEUE_NAME, responseRequest);
            } else {
                walletService.processTransaction(request.getWalletId(), request.getOperationType(), request.getAmount());
                responseRequest = new ResponseRequest(request.getRequestId(), "Транзакция успешно выполнена", null);
                rabbitTemplate.convertAndSend(RESPONSE_QUEUE_NAME, responseRequest);
            }
        } catch (WalletNotFoundException e) {
            responseRequest = new ResponseRequest(request.getRequestId(), null, "Кошелек не найден");
            rabbitTemplate.convertAndSend(RESPONSE_QUEUE_NAME, responseRequest);
        } catch (Exception e) {
            responseRequest = new ResponseRequest(request.getRequestId(), null, e.getMessage());
            rabbitTemplate.convertAndSend(RESPONSE_QUEUE_NAME, responseRequest);
        }
    }
}