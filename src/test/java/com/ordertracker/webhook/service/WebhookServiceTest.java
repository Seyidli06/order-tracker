package com.ordertracker.webhook.service;

import com.ordertracker.support.TestDataFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ordertracker.audit.AuditService;
import com.ordertracker.audit.WebhookAuditLog;
import com.ordertracker.common.enums.OrderStatus;
import com.ordertracker.notification.dto.EmailNotificationRequest;
import com.ordertracker.notification.service.NotificationService;
import com.ordertracker.order.entity.Order;
import com.ordertracker.order.integration.WebhookOrderStatusHandler;
import com.ordertracker.order.repository.OrderRepository;
import com.ordertracker.user.entity.User;
import com.ordertracker.webhook.dto.PaymentWebhookPayload;
import com.ordertracker.webhook.dto.ShipmentWebhookPayload;
import com.ordertracker.webhook.service.WebhookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.*;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    @Mock
    private AuditService auditService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private WebhookOrderStatusHandler webhookOrderStatusHandler;

    @Mock
    private NotificationService notificationService;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private WebhookService webhookService;

    private PaymentWebhookPayload paymentPayload;
    private ShipmentWebhookPayload shipmentPayload;
    private WebhookAuditLog auditLog;
    private Order order;
    private User user;

    @BeforeEach
    void setUp() throws Exception {
        paymentPayload = TestDataFactory.createPaymentWebhookPayload();
        shipmentPayload = TestDataFactory.createShipmentWebhookPayload();
        auditLog = TestDataFactory.createWebhookAuditLog();

        user = User.builder()
                .id(1L)
                .email("test@example.com")
                .password("password")
                .build();

        order = Order.builder()
                .id(1L)
                .externalOrderId("order_test_12345")
                .user(user)
                .status(OrderStatus.PAID)
                .build();

        lenient().when(objectMapper.writeValueAsString(any())).thenReturn("{\"test\":\"payload\"}");
        lenient().when(auditService.logIncomingWebhook(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(auditLog);
        lenient().when(orderRepository.findByExternalOrderId(anyString())).thenReturn(Optional.of(order));
    }

    @Test
    void processPaymentWebhook_PaymentSucceeded_UpdatesOrderToPaid() throws Exception {
        paymentPayload = TestDataFactory.createPaymentWebhookPayload("PAYMENT_SUCCEEDED");

        webhookService.processPaymentWebhook(paymentPayload, "{}");

        verify(auditService).logIncomingWebhook(
                eq(paymentPayload.getEventId()),
                eq(paymentPayload.getEventType()),
                eq(paymentPayload.getSource()),
                anyString(),
                eq("{}")
        );
        verify(webhookOrderStatusHandler).handlePaymentStatus(
                eq(paymentPayload.getPaymentData().getOrderId()),
                eq(paymentPayload.getPaymentData().getStatus()),
                eq(paymentPayload.getEventId())
        );
        verify(notificationService).sendOrderStatusEmail(any(EmailNotificationRequest.class));
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
        verify(webhookOrderStatusHandler).handlePaymentStatus(
                eq(paymentPayload.getPaymentData().getOrderId()),
                eq(paymentPayload.getPaymentData().getStatus()),
                eq(paymentPayload.getEventId())
        );
        verify(notificationService).sendOrderStatusEmail(any(EmailNotificationRequest.class));
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
        verify(webhookOrderStatusHandler).handleShipmentStatus(
                eq(shipmentPayload.getShipmentData().getOrderId()),
                eq(shipmentPayload.getShipmentData().getStatus()),
                eq(shipmentPayload.getEventId())
        );
        verify(notificationService).sendOrderStatusEmail(any(EmailNotificationRequest.class));
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
        verify(webhookOrderStatusHandler).handleShipmentStatus(
                eq(shipmentPayload.getShipmentData().getOrderId()),
                eq(shipmentPayload.getShipmentData().getStatus()),
                eq(shipmentPayload.getEventId())
        );
        verify(notificationService).sendOrderStatusEmail(any(EmailNotificationRequest.class));
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

        verify(webhookOrderStatusHandler, never()).handlePaymentStatus(anyString(), anyString(), anyString());
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

        verify(webhookOrderStatusHandler, never()).handleShipmentStatus(anyString(), anyString(), anyString());
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
        verify(webhookOrderStatusHandler).handlePaymentStatus(
                eq(paymentPayload.getPaymentData().getOrderId()),
                eq(paymentPayload.getPaymentData().getStatus()),
                eq(paymentPayload.getEventId())
        );
        verify(notificationService).sendOrderStatusEmail(any(EmailNotificationRequest.class));
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
        verify(webhookOrderStatusHandler).handleShipmentStatus(
                eq(shipmentPayload.getShipmentData().getOrderId()),
                eq(shipmentPayload.getShipmentData().getStatus()),
                eq(shipmentPayload.getEventId())
        );
        verify(notificationService).sendOrderStatusEmail(any(EmailNotificationRequest.class));
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
        verify(webhookOrderStatusHandler).handlePaymentStatus(
                eq(paymentPayload.getPaymentData().getOrderId()),
                eq(paymentPayload.getPaymentData().getStatus()),
                eq(paymentPayload.getEventId())
        );
        verify(notificationService).sendOrderStatusEmail(any(EmailNotificationRequest.class));
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
        verify(webhookOrderStatusHandler).handleShipmentStatus(
                eq(shipmentPayload.getShipmentData().getOrderId()),
                eq(shipmentPayload.getShipmentData().getStatus()),
                eq(shipmentPayload.getEventId())
        );
        verify(notificationService).sendOrderStatusEmail(any(EmailNotificationRequest.class));
        verify(auditService).markAsProcessed(auditLog.getId());
    }

    @Test
    void processPaymentWebhook_SerializationError_HandledGracefully() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenThrow(new RuntimeException("Serialization error"));

        webhookService.processPaymentWebhook(paymentPayload, "{}");

        verify(auditService, never()).logIncomingWebhook(anyString(), anyString(), anyString(), anyString(), anyString());
        verify(webhookOrderStatusHandler, never()).handlePaymentStatus(anyString(), anyString(), anyString());
    }

    @Test
    void processShipmentWebhook_SerializationError_HandledGracefully() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenThrow(new RuntimeException("Serialization error"));

        webhookService.processShipmentWebhook(shipmentPayload, "{}");

        verify(auditService, never()).logIncomingWebhook(anyString(), anyString(), anyString(), anyString(), anyString());
        verify(webhookOrderStatusHandler, never()).handleShipmentStatus(anyString(), anyString(), anyString());
    }

    @Test
    void processPaymentWebhook_UnsupportedStatus_MarksAuditAsFailed() throws Exception {
        paymentPayload = TestDataFactory.createPaymentWebhookPayload();
        paymentPayload.getPaymentData().setStatus("UNSUPPORTED_STATUS");

        doThrow(new IllegalArgumentException("Unsupported payment status: UNSUPPORTED_STATUS"))
                .when(webhookOrderStatusHandler).handlePaymentStatus(anyString(), anyString(), anyString());

        webhookService.processPaymentWebhook(paymentPayload, "{}");

        verify(auditService).logIncomingWebhook(
                eq(paymentPayload.getEventId()),
                eq(paymentPayload.getEventType()),
                eq(paymentPayload.getSource()),
                anyString(),
                eq("{}")
        );
        verify(auditService).markAsFailed(auditLog.getId(), "Unsupported payment status: UNSUPPORTED_STATUS");
    }

    @Test
    void processShipmentWebhook_UnsupportedStatus_MarksAuditAsFailed() throws Exception {
        shipmentPayload = TestDataFactory.createShipmentWebhookPayload();
        shipmentPayload.getShipmentData().setStatus("UNSUPPORTED_STATUS");

        doThrow(new IllegalArgumentException("Unsupported shipment status: UNSUPPORTED_STATUS"))
                .when(webhookOrderStatusHandler).handleShipmentStatus(anyString(), anyString(), anyString());

        webhookService.processShipmentWebhook(shipmentPayload, "{}");

        verify(auditService).logIncomingWebhook(
                eq(shipmentPayload.getEventId()),
                eq(shipmentPayload.getEventType()),
                eq(shipmentPayload.getSource()),
                anyString(),
                eq("{}")
        );
        verify(auditService).markAsFailed(auditLog.getId(), "Unsupported shipment status: UNSUPPORTED_STATUS");
    }

    @Test
    void processPaymentWebhook_GeneralFailure_MarksAuditAsFailed() throws Exception {
        doThrow(new RuntimeException("General processing error"))
                .when(webhookOrderStatusHandler).handlePaymentStatus(anyString(), anyString(), anyString());

        webhookService.processPaymentWebhook(paymentPayload, "{}");

        verify(auditService).logIncomingWebhook(
                eq(paymentPayload.getEventId()),
                eq(paymentPayload.getEventType()),
                eq(paymentPayload.getSource()),
                anyString(),
                eq("{}")
        );
        verify(auditService).markAsFailed(auditLog.getId(), "General processing error");
    }

    @Test
    void processShipmentWebhook_GeneralFailure_MarksAuditAsFailed() throws Exception {
        doThrow(new RuntimeException("General processing error"))
                .when(webhookOrderStatusHandler).handleShipmentStatus(anyString(), anyString(), anyString());

        webhookService.processShipmentWebhook(shipmentPayload, "{}");

        verify(auditService).logIncomingWebhook(
                eq(shipmentPayload.getEventId()),
                eq(shipmentPayload.getEventType()),
                eq(shipmentPayload.getSource()),
                anyString(),
                eq("{}")
        );
        verify(auditService).markAsFailed(auditLog.getId(), "General processing error");
    }

    @Test
    void processPaymentWebhook_SuccessfulUpdate_SendsNotification() throws Exception {
        webhookService.processPaymentWebhook(paymentPayload, "{}");

        verify(notificationService).sendOrderStatusEmail(any(EmailNotificationRequest.class));
    }

    @Test
    void processShipmentWebhook_SuccessfulUpdate_SendsNotification() throws Exception {
        webhookService.processShipmentWebhook(shipmentPayload, "{}");

        verify(notificationService).sendOrderStatusEmail(any(EmailNotificationRequest.class));
    }

    @Test
    void processPaymentWebhook_FailedUpdate_DoesNotSendNotification() throws Exception {
        doThrow(new RuntimeException("Order update failed"))
                .when(webhookOrderStatusHandler).handlePaymentStatus(anyString(), anyString(), anyString());

        webhookService.processPaymentWebhook(paymentPayload, "{}");

        verify(notificationService, never()).sendOrderStatusEmail(any(EmailNotificationRequest.class));
    }

    @Test
    void processShipmentWebhook_FailedUpdate_DoesNotSendNotification() throws Exception {
        doThrow(new RuntimeException("Order update failed"))
                .when(webhookOrderStatusHandler).handleShipmentStatus(anyString(), anyString(), anyString());

        webhookService.processShipmentWebhook(shipmentPayload, "{}");

        verify(notificationService, never()).sendOrderStatusEmail(any(EmailNotificationRequest.class));
    }

    @Test
    void processPaymentWebhook_MailException_DoesNotBreakMainFlow() throws Exception {
        doThrow(new RuntimeException("Mail server error"))
                .when(notificationService).sendOrderStatusEmail(any(EmailNotificationRequest.class));

        webhookService.processPaymentWebhook(paymentPayload, "{}");

        verify(auditService).markAsProcessed(auditLog.getId());
    }

    @Test
    void processShipmentWebhook_MailException_DoesNotBreakMainFlow() throws Exception {
        doThrow(new RuntimeException("Mail server error"))
                .when(notificationService).sendOrderStatusEmail(any(EmailNotificationRequest.class));

        webhookService.processShipmentWebhook(shipmentPayload, "{}");

        verify(auditService).markAsProcessed(auditLog.getId());
    }

    @Test
    void processPaymentWebhook_OrderNotFound_DoesNotSendNotification() throws Exception {
        when(orderRepository.findByExternalOrderId(anyString())).thenReturn(Optional.empty());

        webhookService.processPaymentWebhook(paymentPayload, "{}");

        verify(notificationService, never()).sendOrderStatusEmail(any(EmailNotificationRequest.class));
    }

    @Test
    void processShipmentWebhook_OrderNotFound_DoesNotSendNotification() throws Exception {
        when(orderRepository.findByExternalOrderId(anyString())).thenReturn(Optional.empty());

        webhookService.processShipmentWebhook(shipmentPayload, "{}");

        verify(notificationService, never()).sendOrderStatusEmail(any(EmailNotificationRequest.class));
    }
}
