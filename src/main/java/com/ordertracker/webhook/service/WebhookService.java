package com.ordertracker.webhook.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ordertracker.audit.AuditService;
import com.ordertracker.order.integration.WebhookOrderStatusHandler;
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

            auditService.markAsProcessed(auditLogId);
            log.info("Successfully processed shipment webhook: eventId={}", payload.getEventId());

        } catch (Exception e) {
            log.error("Failed to process shipment webhook: eventId={}", payload.getEventId(), e);
            if (auditLogId != null) {
                auditService.markAsFailed(auditLogId, e.getMessage());
            }
        }
    }
}
