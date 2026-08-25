package com.ordertracker.webhook.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ordertracker.webhook.dto.PaymentWebhookPayload;
import com.ordertracker.webhook.dto.ShipmentWebhookPayload;
import com.ordertracker.webhook.service.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/webhooks")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Webhook Endpoints", description = "Third-party webhook receivers for payment and shipment events")
public class WebhookController {

    private final WebhookService webhookService;
    private final ObjectMapper objectMapper;

    @PostMapping("/payment")
    @Operation(summary = "Handle payment webhook", description = "Receives and processes payment status updates from third-party payment providers")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Webhook accepted for processing"),
            @ApiResponse(responseCode = "400", description = "Invalid payload format")
    })
    public ResponseEntity<Void> handlePaymentWebhook(
            @Valid @RequestBody PaymentWebhookPayload payload,
            @RequestHeader Map<String, String> headers) {

        log.info("Received payment webhook: eventId={}, eventType={}, orderId={}, amount={}, status={}",
                payload.getEventId(),
                payload.getEventType(),
                payload.getPaymentData().getOrderId(),
                payload.getPaymentData().getAmount(),
                payload.getPaymentData().getStatus());

        try {
            String headersJson = objectMapper.writeValueAsString(headers);
            webhookService.processPaymentWebhook(payload, headersJson);
        } catch (Exception e) {
            log.error("Failed to serialize headers for payment webhook: eventId={}", payload.getEventId(), e);
        }

        return ResponseEntity.ok().build();
    }

    @PostMapping("/shipment")
    @Operation(summary = "Handle shipment webhook", description = "Receives and processes shipment status updates from third-party logistics providers")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Webhook accepted for processing"),
            @ApiResponse(responseCode = "400", description = "Invalid payload format")
    })
    public ResponseEntity<Void> handleShipmentWebhook(
            @Valid @RequestBody ShipmentWebhookPayload payload,
            @RequestHeader Map<String, String> headers) {

        log.info("Received shipment webhook: eventId={}, eventType={}, orderId={}, trackingNumber={}, carrier={}, status={}",
                payload.getEventId(),
                payload.getEventType(),
                payload.getShipmentData().getOrderId(),
                payload.getShipmentData().getTrackingNumber(),
                payload.getShipmentData().getCarrier(),
                payload.getShipmentData().getStatus());

        try {
            String headersJson = objectMapper.writeValueAsString(headers);
            webhookService.processShipmentWebhook(payload, headersJson);
        } catch (Exception e) {
            log.error("Failed to serialize headers for shipment webhook: eventId={}", payload.getEventId(), e);
        }

        return ResponseEntity.ok().build();
    }
}
