package com.javacode.walletmanager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javacode.walletmanager.controller.WalletController;
import com.javacode.walletmanager.model.TransactionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class WalletControllerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private WalletController walletController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(walletController).build();
    }

    @Test
    void testGetBalance_ValidWalletId() throws Exception {
        UUID walletId = UUID.fromString("738c634d-a7c1-48ec-b365-7c07a783c629");
        BigDecimal balance = new BigDecimal("3000.00");
        doAnswer(invocation -> {
            UUID requestId = invocation.getArgument(1, TransactionRequest.class).getRequestId();
            ObjectMapper objectMapper = new ObjectMapper();
            String responseBody = objectMapper.writeValueAsString(Map.of(
                    "walletId", walletId.toString(),
                    "balance", balance
            ));
            walletController.completeResponse(requestId, responseBody);
            return null;
        }).when(rabbitTemplate).convertAndSend(eq("walletQueue"), any(TransactionRequest.class));
        mockMvc.perform(get("/api/v1/wallets/{walletIdStr}", walletId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").doesNotExist()) // Исправленная проверка
                .andExpect(jsonPath("$.data.walletId").value(walletId.toString()))
                .andExpect(jsonPath("$.data.balance").value(balance.doubleValue()));
        verify(rabbitTemplate, times(1)).convertAndSend(eq("walletQueue"), any(TransactionRequest.class));
    }

    @Test
    void testGetBalance_InvalidWalletId() throws Exception {
        mockMvc.perform(get("/api/v1/wallets/{walletIdStr}", "invalid-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Некорректный формат UUID кошелька"));
    }

    @Test
    void testGetBalance_NonExistentWallet() throws Exception {
        UUID walletId = UUID.fromString("738c634d-a7c1-48ec-b365-7c07a783c629");
        doAnswer(invocation -> {
            UUID requestId = invocation.getArgument(1, TransactionRequest.class).getRequestId();
            ObjectMapper objectMapper = new ObjectMapper();
            String responseBody = objectMapper.writeValueAsString(Map.of(
                    "errorMessage", "Кошелек не найден"
            ));
            walletController.completeResponse(requestId, responseBody);
            return null;
        }).when(rabbitTemplate).convertAndSend(eq("walletQueue"), any(TransactionRequest.class));
        mockMvc.perform(get("/api/v1/wallets/{walletIdStr}", walletId.toString()))
                .andExpect(status().isNotFound()) // Ожидаем статус 404
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Кошелек не найден"));
        verify(rabbitTemplate, times(1)).convertAndSend(eq("walletQueue"), any(TransactionRequest.class));
    }

    @Test
    void testProcessTransaction_DepositToExistingWallet() throws Exception {
        UUID walletId = UUID.fromString("738c634d-a7c1-48ec-b365-7c07a783c629");
        BigDecimal initialBalance = new BigDecimal("3000.00");
        BigDecimal depositAmount = new BigDecimal("500.00");
        ObjectMapper objectMapper = new ObjectMapper();
        doAnswer(invocation -> {
            Message message = invocation.getArgument(1, Message.class);
            byte[] body = message.getBody();
            TransactionRequest request = objectMapper.readValue(body, TransactionRequest.class);
            UUID requestId = request.getRequestId();
            String responseBody = objectMapper.writeValueAsString(Map.of(
                    "walletId", walletId.toString(),
                    "balance", initialBalance
            ));
            walletController.completeResponse(requestId, responseBody);
            return null;
        }).when(rabbitTemplate).send(eq("walletQueue"), any(Message.class));
        doAnswer(invocation -> {
            TransactionRequest request = invocation.getArgument(1, TransactionRequest.class);
            UUID requestId = request.getRequestId();
            walletController.completeResponse(requestId, "Транзакция успешно выполнена");
            return null;
        }).when(rabbitTemplate).convertAndSend(eq("walletQueue"), any(TransactionRequest.class));
        mockMvc.perform(post("/api/v1/wallet")
                        .param("walletIdStr", walletId.toString())
                        .param("operationType", "DEPOSIT")
                        .param("amount", depositAmount.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("successfully"));
        verify(rabbitTemplate, times(1)).send(eq("walletQueue"), any(Message.class));
        verify(rabbitTemplate, times(1)).convertAndSend(eq("walletQueue"), any(TransactionRequest.class));
    }

    @Test
    void testProcessTransaction_WithdrawInsufficientFunds() throws Exception {
        UUID walletId = UUID.fromString("738c634d-a7c1-48ec-b365-7c07a783c629");
        BigDecimal initialBalance = new BigDecimal("3000.00");
        BigDecimal withdrawAmount = new BigDecimal("4000.00");
        ObjectMapper objectMapper = new ObjectMapper();
        doAnswer(invocation -> {
            Message message = invocation.getArgument(1, Message.class);
            byte[] body = message.getBody();
            TransactionRequest request = objectMapper.readValue(body, TransactionRequest.class);
            String responseBody = objectMapper.writeValueAsString(Map.of(
                    "walletId", walletId.toString(),
                    "balance", initialBalance
            ));
            walletController.completeResponse(request.getRequestId(), responseBody);
            return null;
        }).when(rabbitTemplate).send(eq("walletQueue"), any(Message.class));
        mockMvc.perform(post("/api/v1/wallet")
                        .param("walletIdStr", walletId.toString())
                        .param("operationType", "WITHDRAW")
                        .param("amount", withdrawAmount.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("недостаточно средств"));
        verify(rabbitTemplate, times(1)).send(eq("walletQueue"), any(Message.class));
        verify(rabbitTemplate, never()).convertAndSend(eq("walletQueue"), any(TransactionRequest.class));
    }

    @Test
    void testProcessTransaction_WithdrawFromExistingWallet() throws Exception {
        UUID walletId = UUID.fromString("738c634d-a7c1-48ec-b365-7c07a783c629");
        BigDecimal initialBalance = new BigDecimal("3000.00");
        BigDecimal withdrawAmount = new BigDecimal("1000.00");
        ObjectMapper objectMapper = new ObjectMapper();
        doAnswer(invocation -> {
            Message message = invocation.getArgument(1, Message.class);
            byte[] body = message.getBody();
            TransactionRequest request = objectMapper.readValue(body, TransactionRequest.class);
            assertEquals("BALANCE", request.getOperationType());
            String responseBody = objectMapper.writeValueAsString(Map.of(
                    "walletId", walletId.toString(),
                    "balance", initialBalance
            ));
            walletController.completeResponse(request.getRequestId(), responseBody);
            return null;
        }).when(rabbitTemplate).send(eq("walletQueue"), any(Message.class));
        doAnswer(invocation -> {
            TransactionRequest request = invocation.getArgument(1, TransactionRequest.class);
            assertEquals("WITHDRAW", request.getOperationType());
            assertEquals(withdrawAmount, request.getAmount());
            walletController.completeResponse(request.getRequestId(), "Транзакция успешно выполнена");
            return null;
        }).when(rabbitTemplate).convertAndSend(eq("walletQueue"), any(TransactionRequest.class));
        mockMvc.perform(post("/api/v1/wallet")
                        .param("walletIdStr", walletId.toString())
                        .param("operationType", "WITHDRAW")
                        .param("amount", withdrawAmount.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("successfully"));
        verify(rabbitTemplate, times(1)).send(eq("walletQueue"), any(Message.class));
        verify(rabbitTemplate, times(1)).convertAndSend(eq("walletQueue"), any(TransactionRequest.class));
    }

    @Test
    void testProcessTransaction_DepositToInvalidUUID() throws Exception {
        String invalidWalletId = "invalid-uuid";
        BigDecimal depositAmount = new BigDecimal("500.00");
        mockMvc.perform(post("/api/v1/wallet")
                        .param("walletIdStr", invalidWalletId)
                        .param("operationType", "DEPOSIT")
                        .param("amount", depositAmount.toString()))
                .andExpect(status().isBadRequest()) // Ожидаем статус 400
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Некорректный формат UUID кошелька"));
        verify(rabbitTemplate, never()).send(eq("walletQueue"), any(Message.class));
        verify(rabbitTemplate, never()).convertAndSend(eq("walletQueue"), any(TransactionRequest.class));
    }

    @Test
    void testProcessTransaction_NegativeAmount() throws Exception {
        UUID walletId = UUID.fromString("738c634d-a7c1-48ec-b365-7c07a783c629");
        BigDecimal negativeAmount = new BigDecimal("-500.00");
        doAnswer(invocation -> {
            Message message = invocation.getArgument(1, Message.class);
            byte[] body = message.getBody();
            TransactionRequest request = new ObjectMapper().readValue(body, TransactionRequest.class);
            String responseBody = new ObjectMapper().writeValueAsString(Map.of(
                    "walletId", walletId.toString(),
                    "balance", new BigDecimal("1000.00") // Пример баланса
            ));
            walletController.completeResponse(request.getRequestId(), responseBody);
            return null;
        }).when(rabbitTemplate).send(eq("walletQueue"), any(Message.class));
        mockMvc.perform(post("/api/v1/wallet")
                        .param("walletIdStr", walletId.toString())
                        .param("operationType", "WITHDRAW")
                        .param("amount", negativeAmount.toString()))
                .andExpect(status().isNotFound()) // Ожидаем статус 404
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("сумма должна быть положительной"));
        verify(rabbitTemplate, never()).convertAndSend(eq("walletQueue"), any(TransactionRequest.class));
    }

    @Test
    void testProcessTransaction_DepositNegativeAmount() throws Exception {
        UUID walletId = UUID.fromString("738c634d-a7c1-48ec-b365-7c07a783c629");
        BigDecimal negativeAmount = new BigDecimal("-500.00");
        doAnswer(invocation -> {
            Message message = invocation.getArgument(1, Message.class);
            byte[] body = message.getBody();
            TransactionRequest request = new ObjectMapper().readValue(body, TransactionRequest.class);
            String responseBody = new ObjectMapper().writeValueAsString(Map.of(
                    "walletId", walletId.toString(),
                    "balance", new BigDecimal("1000.00") // Пример баланса
            ));
            walletController.completeResponse(request.getRequestId(), responseBody);
            return null;
        }).when(rabbitTemplate).send(eq("walletQueue"), any(Message.class));
        mockMvc.perform(post("/api/v1/wallet")
                        .param("walletIdStr", walletId.toString())
                        .param("operationType", "DEPOSIT")
                        .param("amount", negativeAmount.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("сумма должна быть положительной"));

        verify(rabbitTemplate, never()).convertAndSend(eq("walletQueue"), any(TransactionRequest.class));
    }

    @Test
    void testProcessTransaction_InvalidOperationType() throws Exception {
        UUID walletId = UUID.fromString("738c634d-a7c1-48ec-b365-7c07a783c629");
        BigDecimal depositAmount = new BigDecimal("500.00");

        mockMvc.perform(post("/api/v1/wallet")
                        .param("walletIdStr", walletId.toString())
                        .param("operationType", "INVALID_OPERATION")
                        .param("amount", depositAmount.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Некорректный тип операции"));

        verify(rabbitTemplate, never()).send(eq("walletQueue"), any(Message.class));
        verify(rabbitTemplate, never()).convertAndSend(eq("walletQueue"), any(TransactionRequest.class));
    }

    @Test
    void testProcessTransaction_DepositToNonExistentWallet() throws Exception {
        UUID walletId = UUID.fromString("738c634d-a7c1-48ec-b365-7c07a783c629");
        BigDecimal depositAmount = new BigDecimal("500.00");
        ObjectMapper objectMapper = new ObjectMapper();
        doAnswer(invocation -> {
            Message message = invocation.getArgument(1, Message.class);
            byte[] body = message.getBody();
            TransactionRequest request = objectMapper.readValue(body, TransactionRequest.class);
            String responseBody = objectMapper.writeValueAsString(Map.of(
                    "errorMessage", "Кошелек не найден"
            ));
            walletController.completeResponse(request.getRequestId(), responseBody);
            return null;
        }).when(rabbitTemplate).send(eq("walletQueue"), any(Message.class));
        mockMvc.perform(post("/api/v1/wallet")
                        .param("walletIdStr", walletId.toString())
                        .param("operationType", "DEPOSIT")
                        .param("amount", depositAmount.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("невозможно пополнить несуществующий кошелёк"));
        verify(rabbitTemplate, times(1)).send(eq("walletQueue"), any(Message.class));
        verify(rabbitTemplate, never()).convertAndSend(eq("walletQueue"), any(TransactionRequest.class));
    }

    @Test
    void testProcessTransaction_WithdrawZeroAmount() throws Exception {
        UUID walletId = UUID.fromString("738c634d-a7c1-48ec-b365-7c07a783c629");
        BigDecimal zeroAmount = new BigDecimal("0.00");
        doAnswer(invocation -> {
            Message message = invocation.getArgument(1, Message.class);
            byte[] body = message.getBody();
            TransactionRequest request = new ObjectMapper().readValue(body, TransactionRequest.class);
            String responseBody = new ObjectMapper().writeValueAsString(Map.of(
                    "walletId", walletId.toString(),
                    "balance", new BigDecimal("1000.00")
            ));
            walletController.completeResponse(request.getRequestId(), responseBody);
            return null;
        }).when(rabbitTemplate).send(eq("walletQueue"), any(Message.class));
        mockMvc.perform(post("/api/v1/wallet")
                        .param("walletIdStr", walletId.toString())
                        .param("operationType", "WITHDRAW")
                        .param("amount", zeroAmount.toString()))
                .andExpect(status().isNotFound()) // Ожидаем статус 404
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("сумма должна быть положительной"));
        verify(rabbitTemplate, never()).convertAndSend(eq("walletQueue"), any(TransactionRequest.class));
    }
}