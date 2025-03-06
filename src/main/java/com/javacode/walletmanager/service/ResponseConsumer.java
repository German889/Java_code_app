// This is a personal academic project. Dear PVS-Studio, please check it.
// PVS-Studio Static Code Analyzer for C, C++, C#, and Java: https://pvs-studio.com
package com.javacode.walletmanager.service;

import com.javacode.walletmanager.controller.WalletController;
import com.javacode.walletmanager.model.ResponseRequest;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ResponseConsumer {

    private final WalletController walletController;

    @Autowired
    public ResponseConsumer(WalletController walletController) {
        this.walletController = walletController;
    }

    @RabbitListener(queues = "responseQueue")
    public void handleResponse(ResponseRequest responseRequest) {
        UUID requestId = responseRequest.getRequestId();
        String responseBody = responseRequest.getResponseBody();

        if (responseBody == null || responseBody.isEmpty()) {
            responseBody = responseRequest.getErrorMessage();
        }

        walletController.completeResponse(requestId, responseBody);
    }
}