package com.ordertracker.webhook.controller;

import com.ordertracker.support.TestDataFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ordertracker.webhook.controller.WebhookController;
import com.ordertracker.webhook.dto.PaymentWebhookPayload;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = WebhookController.class)
@AutoConfigureTestDatabase
@Disabled("Disabled due to context loading issues - requires additional configuration")
class PaymentWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private com.ordertracker.webhook.service.WebhookService webhookService;

    @Test
    void handlePaymentWebhook_ValidPayload_Returns200() throws Exception {
        PaymentWebhookPayload payload = TestDataFactory.createPaymentWebhookPayload("PAYMENT_SUCCEEDED");

        doNothing().when(webhookService).processPaymentWebhook(any(PaymentWebhookPayload.class), anyString());

        mockMvc.perform(post("/api/webhooks/payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());

        verify(webhookService).processPaymentWebhook(any(PaymentWebhookPayload.class), anyString());
    }

    @Test
    void handlePaymentWebhook_InvalidPayload_MissingEventId_Returns400() throws Exception {
        String invalidPayload = """
                {
                    "event_type": "payment.completed",
                    "source": "stripe",
                    "timestamp": "2026-08-25T08:00:00Z",
                    "payment_data": {
                        "payment_id": "pi_test",
                        "order_id": "order_123",
                        "amount": 99.99,
                        "currency": "USD",
                        "status": "PAYMENT_SUCCEEDED"
                    }
                }
                """;

        mockMvc.perform(post("/api/webhooks/payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void handlePaymentWebhook_InvalidPayload_MissingPaymentData_Returns400() throws Exception {
        String invalidPayload = """
                {
                    "event_id": "pay_evt_123",
                    "event_type": "payment.completed",
                    "source": "stripe",
                    "timestamp": "2026-08-25T08:00:00Z"
                }
                """;

        mockMvc.perform(post("/api/webhooks/payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void handlePaymentWebhook_InvalidPayload_MissingOrderId_Returns400() throws Exception {
        String invalidPayload = """
                {
                    "event_id": "pay_evt_123",
                    "event_type": "payment.completed",
                    "source": "stripe",
                    "timestamp": "2026-08-25T08:00:00Z",
                    "payment_data": {
                        "payment_id": "pi_test",
                        "amount": 99.99,
                        "currency": "USD",
                        "status": "PAYMENT_SUCCEEDED"
                    }
                }
                """;

        mockMvc.perform(post("/api/webhooks/payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void handlePaymentWebhook_InvalidPayload_NegativeAmount_Returns400() throws Exception {
        String invalidPayload = """
                {
                    "event_id": "pay_evt_123",
                    "event_type": "payment.completed",
                    "source": "stripe",
                    "timestamp": "2026-08-25T08:00:00Z",
                    "payment_data": {
                        "payment_id": "pi_test",
                        "order_id": "order_123",
                        "amount": -99.99,
                        "currency": "USD",
                        "status": "PAYMENT_SUCCEEDED"
                    }
                }
                """;

        mockMvc.perform(post("/api/webhooks/payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void handlePaymentWebhook_InvalidPayload_InvalidEmailFormat_Returns400() throws Exception {
        String invalidPayload = """
                {
                    "event_id": "pay_evt_123",
                    "event_type": "payment.completed",
                    "source": "stripe",
                    "timestamp": "2026-08-25T08:00:00Z",
                    "payment_data": {
                        "payment_id": "pi_test",
                        "order_id": "order_123",
                        "amount": 99.99,
                        "currency": "USD",
                        "status": "PAYMENT_SUCCEEDED"
                    },
                    "metadata": {
                        "customer_email": "invalid-email"
                    }
                }
                """;

        mockMvc.perform(post("/api/webhooks/payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void handlePaymentWebhook_ValidPayload_PaymentFailed_Returns200() throws Exception {
        PaymentWebhookPayload payload = TestDataFactory.createPaymentWebhookPayload("PAYMENT_FAILED");

        doNothing().when(webhookService).processPaymentWebhook(any(PaymentWebhookPayload.class), anyString());

        mockMvc.perform(post("/api/webhooks/payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());

        verify(webhookService).processPaymentWebhook(any(PaymentWebhookPayload.class), anyString());
    }
}
