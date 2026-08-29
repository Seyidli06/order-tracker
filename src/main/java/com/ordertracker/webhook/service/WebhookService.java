package com.ordertracker.webhook.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ordertracker.audit.AuditService;
import com.ordertracker.common.enums.OrderStatus;
import com.ordertracker.notification.dto.EmailNotificationRequest;
import com.ordertracker.notification.service.NotificationService;
import com.ordertracker.order.entity.Order;
import com.ordertracker.order.integration.WebhookOrderStatusHandler;
import com.ordertracker.order.repository.OrderRepository;
import com.ordertracker.webhook.dto.PaymentWebhookPayload;
import com.ordertracker.webhook.dto.ShipmentWebhookPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    private final WebhookOrderStatusHandler webhookOrderStatusHandler;
    private final NotificationService notificationService;
    private final OrderRepository orderRepository;

    @Async
    public void processPaymentWebhook(PaymentWebhookPayload payload, String headersJson) {
        Long auditLogId = null;
        try {
            auditLogId = auditService.logIncomingWebhook(
                    payload.getEventId(),
                    payload.getEventType(),
                    payload.getSource(),
                    objectMapper.writeValueAsString(payload),
                    headersJson
            ).getId();

            log.info("Processing payment webhook: eventId={}, orderId={}, amount={}, status={}",
                    payload.getEventId(),
                    payload.getPaymentData().getOrderId(),
                    payload.getPaymentData().getAmount(),
                    payload.getPaymentData().getStatus());

            webhookOrderStatusHandler.handlePaymentStatus(
                    payload.getPaymentData().getOrderId(),
                    payload.getPaymentData().getStatus(),
                    payload.getEventId()
            );

            // Send notification after successful order status update
            sendOrderStatusNotification(payload.getPaymentData().getOrderId(), payload.getEventId());

            auditService.markAsProcessed(auditLogId);
            log.info("Successfully processed payment webhook: eventId={}", payload.getEventId());

        } catch (Exception e) {
            log.error("Failed to process payment webhook: eventId={}", payload.getEventId(), e);
            if (auditLogId != null) {
                auditService.markAsFailed(auditLogId, e.getMessage());
            }
        }
    }

    @Async
    public void processShipmentWebhook(ShipmentWebhookPayload payload, String headersJson) {
        Long auditLogId = null;
        try {
            auditLogId = auditService.logIncomingWebhook(
                    payload.getEventId(),
                    payload.getEventType(),
                    payload.getSource(),
                    objectMapper.writeValueAsString(payload),
                    headersJson
            ).getId();

            log.info("Processing shipment webhook: eventId={}, orderId={}, trackingNumber={}, carrier={}, status={}",
                    payload.getEventId(),
                    payload.getShipmentData().getOrderId(),
                    payload.getShipmentData().getTrackingNumber(),
                    payload.getShipmentData().getCarrier(),
                    payload.getShipmentData().getStatus());

            webhookOrderStatusHandler.handleShipmentStatus(
                    payload.getShipmentData().getOrderId(),
                    payload.getShipmentData().getStatus(),
                    payload.getEventId()
            );

            // Send notification after successful order status update
            sendOrderStatusNotification(payload.getShipmentData().getOrderId(), payload.getEventId());

            auditService.markAsProcessed(auditLogId);
            log.info("Successfully processed shipment webhook: eventId={}", payload.getEventId());

        } catch (Exception e) {
            log.error("Failed to process shipment webhook: eventId={}", payload.getEventId(), e);
            if (auditLogId != null) {
                auditService.markAsFailed(auditLogId, e.getMessage());
            }
        }
    }

    @Async
    private void sendOrderStatusNotification(String externalOrderId, String eventId) {
        try {
            Order order = orderRepository.findByExternalOrderId(externalOrderId)
                    .orElse(null);

            if (order == null) {
                log.warn("Order not found for notification: externalOrderId={}, eventId={}", externalOrderId, eventId);
                return;
            }

            String recipientEmail = order.getUser().getEmail();
            OrderStatus orderStatus = order.getStatus();

            String subject = "Order Status Update - " + externalOrderId;
            String body = String.format(
                    "Your order %s has been updated to status: %s",
                    externalOrderId,
                    orderStatus
            );

            EmailNotificationRequest request = EmailNotificationRequest.builder()
                    .recipientEmail(recipientEmail)
                    .subject(subject)
                    .body(body)
                    .orderId(externalOrderId)
                    .build();

            notificationService.sendOrderStatusEmail(request);

            log.info("Order status notification sent: orderId={}, status={}, email={}",
                    externalOrderId, orderStatus, recipientEmail);

        } catch (Exception e) {
            log.error("Failed to send order status notification: externalOrderId={}, eventId={}",
                    externalOrderId, eventId, e);
            // Don't rethrow - notification failure should not affect the main webhook flow
        }
    }
}
