package com.ordertracker.webhook.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ordertracker.audit.AuditService;
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

    private final WebhookOrderStatusHandler
            webhookOrderStatusHandler;

    private final NotificationService
            notificationService;

    private final OrderRepository
            orderRepository;

    @Async
    public void processPaymentWebhook(
            PaymentWebhookPayload payload,
            String headersJson
    ) {

        Long auditLogId = null;

        try {

            auditLogId =
                    auditService
                            .logIncomingWebhook(
                                    payload.getEventId(),
                                    payload.getEventType(),
                                    payload.getSource(),
                                    objectMapper.writeValueAsString(
                                            payload
                                    ),
                                    headersJson
                            )
                            .getId();

            String externalOrderId =
                    payload
                            .getPaymentData()
                            .getOrderId();

            log.info(
                    "Processing payment webhook: eventId={}, orderId={}, amount={}, status={}",
                    payload.getEventId(),
                    externalOrderId,
                    payload
                            .getPaymentData()
                            .getAmount(),
                    payload
                            .getPaymentData()
                            .getStatus()
            );

            webhookOrderStatusHandler
                    .handlePaymentStatus(
                            externalOrderId,
                            payload
                                    .getPaymentData()
                                    .getStatus(),
                            payload.getEventId()
                    );

            /*
             * Order status update uğurlu olduqdan sonra
             * notification göndərilir.
             *
             * Notification failure webhook processing-i
             * FAILED etməməlidir.
             */
            sendOrderStatusNotification(
                    externalOrderId,
                    payload.getEventId()
            );

            auditService.markAsProcessed(
                    auditLogId
            );

            log.info(
                    "Successfully processed payment webhook: eventId={}",
                    payload.getEventId()
            );

        } catch (Exception e) {

            log.error(
                    "Failed to process payment webhook: eventId={}",
                    payload.getEventId(),
                    e
            );

            if (auditLogId != null) {

                auditService.markAsFailed(
                        auditLogId,
                        e.getMessage()
                );
            }
        }
    }

    @Async
    public void processShipmentWebhook(
            ShipmentWebhookPayload payload,
            String headersJson
    ) {

        Long auditLogId = null;

        try {

            auditLogId =
                    auditService
                            .logIncomingWebhook(
                                    payload.getEventId(),
                                    payload.getEventType(),
                                    payload.getSource(),
                                    objectMapper.writeValueAsString(
                                            payload
                                    ),
                                    headersJson
                            )
                            .getId();

            String externalOrderId =
                    payload
                            .getShipmentData()
                            .getOrderId();

            log.info(
                    "Processing shipment webhook: eventId={}, orderId={}, trackingNumber={}, carrier={}, status={}",
                    payload.getEventId(),
                    externalOrderId,
                    payload
                            .getShipmentData()
                            .getTrackingNumber(),
                    payload
                            .getShipmentData()
                            .getCarrier(),
                    payload
                            .getShipmentData()
                            .getStatus()
            );

            webhookOrderStatusHandler
                    .handleShipmentStatus(
                            externalOrderId,
                            payload
                                    .getShipmentData()
                                    .getStatus(),
                            payload.getEventId()
                    );

            sendOrderStatusNotification(
                    externalOrderId,
                    payload.getEventId()
            );

            auditService.markAsProcessed(
                    auditLogId
            );

            log.info(
                    "Successfully processed shipment webhook: eventId={}",
                    payload.getEventId()
            );

        } catch (Exception e) {

            log.error(
                    "Failed to process shipment webhook: eventId={}",
                    payload.getEventId(),
                    e
            );

            if (auditLogId != null) {

                auditService.markAsFailed(
                        auditLogId,
                        e.getMessage()
                );
            }
        }
    }

    private void sendOrderStatusNotification(
            String externalOrderId,
            String eventId
    ) {

        try {

            Order order =
                    orderRepository
                            .findWithUserByExternalOrderId(
                                    externalOrderId
                            )
                            .orElse(null);

            if (order == null) {

                log.warn(
                        "Skipping notification because order was not found: externalOrderId={}, eventId={}",
                        externalOrderId,
                        eventId
                );

                return;
            }

            if (order.getUser() == null) {

                log.warn(
                        "Skipping notification because order has no user: externalOrderId={}, eventId={}",
                        externalOrderId,
                        eventId
                );

                return;
            }

            String recipientEmail =
                    order
                            .getUser()
                            .getEmail();

            if (recipientEmail == null
                    || recipientEmail.isBlank()) {

                log.warn(
                        "Skipping notification because recipient email is missing: externalOrderId={}, eventId={}",
                        externalOrderId,
                        eventId
                );

                return;
            }

            String subject =
                    "Order Status Update - "
                            + externalOrderId;

            String body =
                    "Your order "
                            + externalOrderId
                            + " has been updated to status: "
                            + order.getStatus();

            EmailNotificationRequest request =
                    EmailNotificationRequest
                            .builder()
                            .recipientEmail(
                                    recipientEmail
                            )
                            .subject(
                                    subject
                            )
                            .body(
                                    body
                            )
                            .orderId(
                                    externalOrderId
                            )
                            .build();

            notificationService
                    .sendOrderStatusEmail(
                            request
                    );

            log.info(
                    "Order status notification scheduled: orderId={}, status={}, recipient={}",
                    externalOrderId,
                    order.getStatus(),
                    recipientEmail
            );

        } catch (Exception e) {

            /*
             * Email problemi əsas webhook processing-i
             * uğursuz etməməlidir.
             */
            log.error(
                    "Failed to prepare order status notification: externalOrderId={}, eventId={}",
                    externalOrderId,
                    eventId,
                    e
            );
        }
    }
}