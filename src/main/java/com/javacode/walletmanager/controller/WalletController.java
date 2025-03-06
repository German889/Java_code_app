package com.javacode.walletmanager.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javacode.walletmanager.model.ApiResponse;
import com.javacode.walletmanager.model.TransactionRequest;
import com.javacode.walletmanager.model.WalletResponse;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1")
public class WalletController {
    private static final String REQUEST_QUEUE_NAME = "walletQueue";
    private final RabbitTemplate rabbitTemplate;
    private final Map<UUID, CompletableFuture<String>> responseFutures = new ConcurrentHashMap<>();

    @Autowired
    public WalletController(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @PostMapping("/wallet")
    public ResponseEntity<ApiResponse<Object>> processTransaction(
            @RequestParam String walletIdStr,
            @RequestParam String operationType,
            @RequestParam BigDecimal amount) {

        UUID requestGETId = UUID.randomUUID();
        UUID requestPOSTId = UUID.randomUUID();

        CompletableFuture<String> performOperation = new CompletableFuture<>();
        responseFutures.put(requestPOSTId, performOperation);
        CompletableFuture<String> checkWalletExist = new CompletableFuture<>();
        responseFutures.put(requestGETId, checkWalletExist);
        try {
            UUID walletId = UUID.fromString(walletIdStr);
            Set<String> validOperations = Set.of("DEPOSIT", "WITHDRAW");
            if (!validOperations.contains(operationType)) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(new ApiResponse<>("Некорректный тип операции"));
            }

            TransactionRequest walletCheckRequest = new TransactionRequest(requestGETId, walletId, "BALANCE", BigDecimal.ZERO);
            ObjectMapper objectMapper = new ObjectMapper();
            MessageProperties messageProperties = new MessageProperties();
            messageProperties.setPriority(10);
            Message message = new Message(objectMapper.writeValueAsBytes(walletCheckRequest), messageProperties);

            rabbitTemplate.send(REQUEST_QUEUE_NAME, message);
            String responseBody = checkWalletExist.join();
            if (responseBody.contains("Кошелек не найден")) {
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>("невозможно пополнить несуществующий кошелёк"));
            }
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>("сумма должна быть положительной"));
            }
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {
            });
            BigDecimal balance = new BigDecimal(responseMap.get("balance").toString());
            if (balance.compareTo(amount) <= 0 && operationType.equals("WITHDRAW")) {
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>("недостаточно средств"));
            }
            TransactionRequest walletPerformRequest = new TransactionRequest(requestPOSTId, walletId, operationType, amount);
            rabbitTemplate.convertAndSend(REQUEST_QUEUE_NAME, walletPerformRequest);
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>("Некорректный формат UUID кошелька"));
        } catch (JsonProcessingException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>("ошибка парсинга ответа JSON"));
        } finally {
            responseFutures.remove(requestGETId);
            responseFutures.remove(requestPOSTId);
        }
        return ResponseEntity.status(HttpStatus.OK).body(new ApiResponse<>());
    }

    @GetMapping("/wallets/{walletIdStr}")
    public ResponseEntity<ApiResponse<WalletResponse>> getBalance(@PathVariable String walletIdStr) {
        UUID requestId = UUID.randomUUID();
        CompletableFuture<String> future = new CompletableFuture<>();
        responseFutures.put(requestId, future);

        try {
            UUID walletId = UUID.fromString(walletIdStr);
            TransactionRequest request = new TransactionRequest(requestId, walletId, "BALANCE", BigDecimal.ZERO);
            rabbitTemplate.convertAndSend(REQUEST_QUEUE_NAME, request);
            String responseBody = future.join();
            ObjectMapper objectMapper = new ObjectMapper();
            if (responseBody != null && responseBody.startsWith("{") && responseBody.endsWith("}")) {
                JsonNode jsonNode = objectMapper.readTree(responseBody);
                if (jsonNode.has("errorMessage")) {
                    String errorMessage = jsonNode.get("errorMessage").asText();
                    return ResponseEntity
                            .status(HttpStatus.NOT_FOUND)
                            .body(new ApiResponse<>(errorMessage));
                }

                Map<String, Object> responseMap = objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {
                });
                UUID walletIdFromResponse = UUID.fromString((String) responseMap.get("walletId"));
                BigDecimal balance = new BigDecimal(responseMap.get("balance").toString());
                WalletResponse response = new WalletResponse(walletIdFromResponse, balance);

                return ResponseEntity
                        .status(HttpStatus.OK)
                        .body(new ApiResponse<>(response));
            } else {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(new ApiResponse<>(responseBody));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>("Некорректный формат UUID кошелька"));
        } catch (JsonProcessingException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>("Некорректный формат JSON в ответе"));
        } finally {
            responseFutures.remove(requestId);
        }
    }

    public void completeResponse(UUID requestId, String responseBody) {
        CompletableFuture<String> future = responseFutures.get(requestId);
        if (future != null) {
            future.complete(responseBody);
        }
    }
}