package com.ordertracker.webhook.controller;

import com.ordertracker.webhook.dto.PaymentWebhookPayload;
import com.ordertracker.webhook.dto.ShipmentWebhookPayload;
import jakarta.validation.Valid;
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
public class WebhookController {

    @PostMapping("/payment")
    public ResponseEntity<Void> handlePaymentWebhook(
            @Valid @RequestBody PaymentWebhookPayload payload,
            @RequestHeader Map<String, String> headers) {

        log.info("Received payment webhook: eventId={}, eventType={}, orderId={}, amount={}, status={}",
                payload.getEventId(),
                payload.getEventType(),
                payload.getPaymentData().getOrderId(),
                payload.getPaymentData().getAmount(),
                payload.getPaymentData().getStatus());

        log.debug("Payment webhook headers: {}", headers);
        log.debug("Payment webhook payload: {}", payload);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/shipment")
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

        log.debug("Shipment webhook headers: {}", headers);
        log.debug("Shipment webhook payload: {}", payload);

        return ResponseEntity.ok().build();
    }
}
