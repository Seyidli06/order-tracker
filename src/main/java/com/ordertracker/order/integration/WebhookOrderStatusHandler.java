package com.ordertracker.order.integration;

import com.ordertracker.common.enums.OrderStatus;
import com.ordertracker.common.enums.StatusChangeSource;
import com.ordertracker.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@RequiredArgsConstructor
public class WebhookOrderStatusHandler {

    private final OrderService orderService;

    public void handlePaymentStatus(
            String externalOrderId,
            String paymentStatus,
            String eventId
    ) {
        OrderStatus newStatus =
                mapPaymentStatus(paymentStatus);

        orderService.updateStatus(
                externalOrderId,
                newStatus,
                StatusChangeSource.PAYMENT_WEBHOOK,
                eventId
        );
    }

    public void handleShipmentStatus(
            String externalOrderId,
            String shipmentStatus,
            String eventId
    ) {
        OrderStatus newStatus =
                mapShipmentStatus(shipmentStatus);

        orderService.updateStatus(
                externalOrderId,
                newStatus,
                StatusChangeSource.SHIPMENT_WEBHOOK,
                eventId
        );
    }

    private OrderStatus mapPaymentStatus(
            String paymentStatus
    ) {
        String normalizedStatus =
                normalizeStatus(
                        paymentStatus,
                        "Payment status"
                );

        return switch (normalizedStatus) {
            case "completed",
                 "succeeded",
                 "paid",
                 "payment_completed",
                 "payment_succeeded",
                 "payment_paid" ->
                    OrderStatus.PAID;

            case "failed",
                 "declined",
                 "cancelled",
                 "payment_failed",
                 "payment_declined",
                 "payment_cancelled" ->
                    OrderStatus.PAYMENT_FAILED;

            case "pending",
                 "processing",
                 "payment_pending",
                 "payment_processing" ->
                    OrderStatus.PAYMENT_PENDING;

            case "refunded",
                 "payment_refunded" ->
                    OrderStatus.REFUNDED;

            default ->
                    throw new IllegalArgumentException(
                            "Unsupported payment status: "
                                    + paymentStatus
                    );
        };
    }

    private OrderStatus mapShipmentStatus(
            String shipmentStatus
    ) {
        String normalizedStatus =
                normalizeStatus(
                        shipmentStatus,
                        "Shipment status"
                );

        return switch (normalizedStatus) {
            case "shipped",
                 "in_transit",
                 "on_the_way",
                 "shipment_shipped",
                 "shipment_in_transit" ->
                    OrderStatus.SHIPPED;

            case "out_for_delivery",
                 "shipment_out_for_delivery" ->
                    OrderStatus.OUT_FOR_DELIVERY;

            case "delivered",
                 "shipment_delivered" ->
                    OrderStatus.DELIVERED;

            case "cancelled",
                 "shipment_cancelled" ->
                    OrderStatus.CANCELLED;

            case "returned",
                 "shipment_returned" ->
                    OrderStatus.RETURNED;

            case "pending",
                 "label_created",
                 "shipment_pending",
                 "shipment_label_created" ->
                    OrderStatus.PROCESSING;

            default ->
                    throw new IllegalArgumentException(
                            "Unsupported shipment status: "
                                    + shipmentStatus
                    );
        };
    }

    private String normalizeStatus(
            String status,
            String fieldName
    ) {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " must not be blank"
            );
        }

        return status
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}