package com.ordertracker.webhook.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ordertracker.audit.AuditService;
import com.ordertracker.audit.WebhookAuditLog;
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

            String orderStatus = determineOrderStatusFromPayment(payload.getPaymentData().getStatus());
            log.info("Determined order status '{}' for order {} based on payment status {}",
                    orderStatus, payload.getPaymentData().getOrderId(), payload.getPaymentData().getStatus());

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

            String orderStatus = determineOrderStatusFromShipment(payload.getShipmentData().getStatus());
            log.info("Determined order status '{}' for order {} based on shipment status {}",
                    orderStatus, payload.getShipmentData().getOrderId(), payload.getShipmentData().getStatus());

            auditService.markAsProcessed(auditLogId);
            log.info("Successfully processed shipment webhook: eventId={}", payload.getEventId());

        } catch (Exception e) {
            log.error("Failed to process shipment webhook: eventId={}", payload.getEventId(), e);
            if (auditLogId != null) {
                auditService.markAsFailed(auditLogId, e.getMessage());
            }
        }
    }

    private String determineOrderStatusFromPayment(String paymentStatus) {
        return switch (paymentStatus.toLowerCase()) {
            case "completed", "succeeded", "paid" -> "PAID";
            case "failed", "declined", "cancelled" -> "PAYMENT_FAILED";
            case "pending", "processing" -> "PAYMENT_PENDING";
            case "refunded" -> "REFUNDED";
            default -> "PAYMENT_UNKNOWN";
        };
    }

    private String determineOrderStatusFromShipment(String shipmentStatus) {
        return switch (shipmentStatus.toLowerCase()) {
            case "shipped", "in_transit", "on_the_way" -> "SHIPPED";
            case "delivered" -> "DELIVERED";
            case "out_for_delivery" -> "OUT_FOR_DELIVERY";
            case "cancelled" -> "CANCELLED";
            case "returned" -> "RETURNED";
            case "pending", "label_created" -> "PROCESSING";
            default -> "SHIPMENT_UNKNOWN";
        };
    }
}
