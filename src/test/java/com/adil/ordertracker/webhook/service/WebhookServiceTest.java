package com.adil.ordertracker.webhook.service;

import com.adil.ordertracker.support.TestDataFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ordertracker.audit.AuditService;
import com.ordertracker.audit.WebhookAuditLog;
import com.ordertracker.webhook.dto.PaymentWebhookPayload;
import com.ordertracker.webhook.dto.ShipmentWebhookPayload;
import com.ordertracker.webhook.service.WebhookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    @Mock
    private AuditService auditService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private WebhookService webhookService;

    private PaymentWebhookPayload paymentPayload;
    private ShipmentWebhookPayload shipmentPayload;
    private WebhookAuditLog auditLog;

    @BeforeEach
    void setUp() throws Exception {
        paymentPayload = TestDataFactory.createPaymentWebhookPayload();
        shipmentPayload = TestDataFactory.createShipmentWebhookPayload();
        auditLog = TestDataFactory.createWebhookAuditLog();

        when(objectMapper.writeValueAsString(any())).thenReturn("{\"test\":\"payload\"}");
        when(auditService.logIncomingWebhook(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(auditLog);
    }

    @Test
    void processPaymentWebhook_PaymentSucceeded_UpdatesOrderToPaid() throws Exception {
        paymentPayload = TestDataFactory.createPaymentWebhookPayload("PAYMENT_SUCCEEDED");

        webhookService.processPaymentWebhook(paymentPayload, "{}");

        ArgumentCaptor<String> statusCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditService).logIncomingWebhook(
                eq(paymentPayload.getEventId()),
                eq(paymentPayload.getEventType()),
                eq(paymentPayload.getSource()),
                anyString(),
                eq("{}")
        );
        verify(auditService).markAsProcessed(auditLog.getId());
    }

    @Test
    void processPaymentWebhook_PaymentFailed_UpdatesOrderToPaymentFailed() throws Exception {
        paymentPayload = TestDataFactory.createPaymentWebhookPayload("PAYMENT_FAILED");

        webhookService.processPaymentWebhook(paymentPayload, "{}");

        verify(auditService).logIncomingWebhook(
                eq(paymentPayload.getEventId()),
                eq(paymentPayload.getEventType()),
                eq(paymentPayload.getSource()),
                anyString(),
                eq("{}")
        );
        verify(auditService).markAsProcessed(auditLog.getId());
    }

    @Test
    void processShipmentWebhook_Shipped_UpdatesOrderToShipped() throws Exception {
        shipmentPayload = TestDataFactory.createShipmentWebhookPayload("SHIPPED");

        webhookService.processShipmentWebhook(shipmentPayload, "{}");

        verify(auditService).logIncomingWebhook(
                eq(shipmentPayload.getEventId()),
                eq(shipmentPayload.getEventType()),
                eq(shipmentPayload.getSource()),
                anyString(),
                eq("{}")
        );
        verify(auditService).markAsProcessed(auditLog.getId());
    }

    @Test
    void processShipmentWebhook_Delivered_UpdatesOrderToDelivered() throws Exception {
        shipmentPayload = TestDataFactory.createShipmentWebhookPayload("DELIVERED");

        webhookService.processShipmentWebhook(shipmentPayload, "{}");

        verify(auditService).logIncomingWebhook(
                eq(shipmentPayload.getEventId()),
                eq(shipmentPayload.getEventType()),
                eq(shipmentPayload.getSource()),
                anyString(),
                eq("{}")
        );
        verify(auditService).markAsProcessed(auditLog.getId());
    }

    @Test
    void processPaymentWebhook_DuplicateEvent_ThrowsException() throws Exception {
        String duplicateEventId = "duplicate_evt_123";
        paymentPayload = TestDataFactory.createPaymentWebhookPayload();
        paymentPayload.setEventId(duplicateEventId);

        when(auditService.logIncomingWebhook(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new IllegalArgumentException("Webhook event with ID " + duplicateEventId + " already exists"));

        webhookService.processPaymentWebhook(paymentPayload, "{}");

        verify(auditService, never()).markAsProcessed(any());
    }

    @Test
    void processShipmentWebhook_DuplicateEvent_ThrowsException() throws Exception {
        String duplicateEventId = "duplicate_evt_456";
        shipmentPayload = TestDataFactory.createShipmentWebhookPayload();
        shipmentPayload.setEventId(duplicateEventId);

        when(auditService.logIncomingWebhook(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new IllegalArgumentException("Webhook event with ID " + duplicateEventId + " already exists"));

        webhookService.processShipmentWebhook(shipmentPayload, "{}");

        verify(auditService, never()).markAsProcessed(any());
    }

    @Test
    void processPaymentWebhook_UnknownOrderId_MarksAsFailed() throws Exception {
        paymentPayload = TestDataFactory.createPaymentWebhookPayload();
        paymentPayload.getPaymentData().setOrderId("unknown_order_999");

        webhookService.processPaymentWebhook(paymentPayload, "{}");

        verify(auditService).logIncomingWebhook(
                eq(paymentPayload.getEventId()),
                eq(paymentPayload.getEventType()),
                eq(paymentPayload.getSource()),
                anyString(),
                eq("{}")
        );
        verify(auditService).markAsProcessed(auditLog.getId());
    }

    @Test
    void processShipmentWebhook_UnknownOrderId_MarksAsFailed() throws Exception {
        shipmentPayload = TestDataFactory.createShipmentWebhookPayload();
        shipmentPayload.getShipmentData().setOrderId("unknown_order_999");

        webhookService.processShipmentWebhook(shipmentPayload, "{}");

        verify(auditService).logIncomingWebhook(
                eq(shipmentPayload.getEventId()),
                eq(shipmentPayload.getEventType()),
                eq(shipmentPayload.getSource()),
                anyString(),
                eq("{}")
        );
        verify(auditService).markAsProcessed(auditLog.getId());
    }

    @Test
    void processPaymentWebhook_PaymentPending_UpdatesOrderToPaymentPending() throws Exception {
        paymentPayload = TestDataFactory.createPaymentWebhookPayload("PENDING");

        webhookService.processPaymentWebhook(paymentPayload, "{}");

        verify(auditService).logIncomingWebhook(
                eq(paymentPayload.getEventId()),
                eq(paymentPayload.getEventType()),
                eq(paymentPayload.getSource()),
                anyString(),
                eq("{}")
        );
        verify(auditService).markAsProcessed(auditLog.getId());
    }

    @Test
    void processShipmentWebhook_OutForDelivery_UpdatesOrderToOutForDelivery() throws Exception {
        shipmentPayload = TestDataFactory.createShipmentWebhookPayload("OUT_FOR_DELIVERY");

        webhookService.processShipmentWebhook(shipmentPayload, "{}");

        verify(auditService).logIncomingWebhook(
                eq(shipmentPayload.getEventId()),
                eq(shipmentPayload.getEventType()),
                eq(shipmentPayload.getSource()),
                anyString(),
                eq("{}")
        );
        verify(auditService).markAsProcessed(auditLog.getId());
    }

    @Test
    void processPaymentWebhook_SerializationError_HandledGracefully() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenThrow(new RuntimeException("Serialization error"));

        webhookService.processPaymentWebhook(paymentPayload, "{}");

        verify(auditService, never()).logIncomingWebhook(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void processShipmentWebhook_SerializationError_HandledGracefully() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenThrow(new RuntimeException("Serialization error"));

        webhookService.processShipmentWebhook(shipmentPayload, "{}");

        verify(auditService, never()).logIncomingWebhook(anyString(), anyString(), anyString(), anyString(), anyString());
    }
}
