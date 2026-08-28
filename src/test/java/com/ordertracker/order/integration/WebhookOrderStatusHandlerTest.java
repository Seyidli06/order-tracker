package com.ordertracker.order.integration;

import com.ordertracker.common.enums.OrderStatus;
import com.ordertracker.common.enums.StatusChangeSource;
import com.ordertracker.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class WebhookOrderStatusHandlerTest {

    @Mock
    private OrderService orderService;

    private WebhookOrderStatusHandler handler;

    @BeforeEach
    void setUp() {
        handler =
                new WebhookOrderStatusHandler(
                        orderService
                );
    }

    @Test
    void shouldMapPaymentSucceededToPaid() {
        handler.handlePaymentStatus(
                "ord_123",
                "PAYMENT_SUCCEEDED",
                "evt_payment_1"
        );

        verify(orderService)
                .updateStatus(
                        "ord_123",
                        OrderStatus.PAID,
                        StatusChangeSource.PAYMENT_WEBHOOK,
                        "evt_payment_1"
                );
    }

    @Test
    void shouldMapPaymentFailedToPaymentFailed() {
        handler.handlePaymentStatus(
                "ord_123",
                "PAYMENT_FAILED",
                "evt_payment_2"
        );

        verify(orderService)
                .updateStatus(
                        "ord_123",
                        OrderStatus.PAYMENT_FAILED,
                        StatusChangeSource.PAYMENT_WEBHOOK,
                        "evt_payment_2"
                );
    }

    @Test
    void shouldMapPaymentPendingToPaymentPending() {
        handler.handlePaymentStatus(
                "ord_123",
                "PENDING",
                "evt_payment_3"
        );

        verify(orderService)
                .updateStatus(
                        "ord_123",
                        OrderStatus.PAYMENT_PENDING,
                        StatusChangeSource.PAYMENT_WEBHOOK,
                        "evt_payment_3"
                );
    }

    @Test
    void shouldMapPaymentRefundedToRefunded() {
        handler.handlePaymentStatus(
                "ord_123",
                "REFUNDED",
                "evt_payment_4"
        );

        verify(orderService)
                .updateStatus(
                        "ord_123",
                        OrderStatus.REFUNDED,
                        StatusChangeSource.PAYMENT_WEBHOOK,
                        "evt_payment_4"
                );
    }

    @Test
    void shouldMapShipmentShippedToShipped() {
        handler.handleShipmentStatus(
                "ord_123",
                "SHIPPED",
                "evt_shipment_1"
        );

        verify(orderService)
                .updateStatus(
                        "ord_123",
                        OrderStatus.SHIPPED,
                        StatusChangeSource.SHIPMENT_WEBHOOK,
                        "evt_shipment_1"
                );
    }

    @Test
    void shouldMapOutForDelivery() {
        handler.handleShipmentStatus(
                "ord_123",
                "OUT_FOR_DELIVERY",
                "evt_shipment_2"
        );

        verify(orderService)
                .updateStatus(
                        "ord_123",
                        OrderStatus.OUT_FOR_DELIVERY,
                        StatusChangeSource.SHIPMENT_WEBHOOK,
                        "evt_shipment_2"
                );
    }

    @Test
    void shouldMapDelivered() {
        handler.handleShipmentStatus(
                "ord_123",
                "DELIVERED",
                "evt_shipment_3"
        );

        verify(orderService)
                .updateStatus(
                        "ord_123",
                        OrderStatus.DELIVERED,
                        StatusChangeSource.SHIPMENT_WEBHOOK,
                        "evt_shipment_3"
                );
    }

    @Test
    void shouldMapShipmentPendingToProcessing() {
        handler.handleShipmentStatus(
                "ord_123",
                "LABEL_CREATED",
                "evt_shipment_4"
        );

        verify(orderService)
                .updateStatus(
                        "ord_123",
                        OrderStatus.PROCESSING,
                        StatusChangeSource.SHIPMENT_WEBHOOK,
                        "evt_shipment_4"
                );
    }

    @Test
    void shouldNormalizeStatus() {
        handler.handlePaymentStatus(
                "ord_123",
                "  PAYMENT_SUCCEEDED  ",
                "evt_payment_5"
        );

        verify(orderService)
                .updateStatus(
                        "ord_123",
                        OrderStatus.PAID,
                        StatusChangeSource.PAYMENT_WEBHOOK,
                        "evt_payment_5"
                );
    }

    @Test
    void shouldRejectUnsupportedPaymentStatus() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                handler.handlePaymentStatus(
                                        "ord_123",
                                        "UNKNOWN_PAYMENT_STATUS",
                                        "evt_payment_unknown"
                                )
                );

        assertEquals(
                "Unsupported payment status: UNKNOWN_PAYMENT_STATUS",
                exception.getMessage()
        );

        verifyNoInteractions(orderService);
    }

    @Test
    void shouldRejectUnsupportedShipmentStatus() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                handler.handleShipmentStatus(
                                        "ord_123",
                                        "UNKNOWN_SHIPMENT_STATUS",
                                        "evt_shipment_unknown"
                                )
                );

        assertEquals(
                "Unsupported shipment status: UNKNOWN_SHIPMENT_STATUS",
                exception.getMessage()
        );

        verifyNoInteractions(orderService);
    }

    @Test
    void shouldRejectBlankPaymentStatus() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                handler.handlePaymentStatus(
                                        "ord_123",
                                        " ",
                                        "evt_payment_blank"
                                )
                );

        assertEquals(
                "Payment status must not be blank",
                exception.getMessage()
        );

        verifyNoInteractions(orderService);
    }

    @Test
    void shouldRejectNullShipmentStatus() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                handler.handleShipmentStatus(
                                        "ord_123",
                                        null,
                                        "evt_shipment_null"
                                )
                );

        assertEquals(
                "Shipment status must not be blank",
                exception.getMessage()
        );

        verifyNoInteractions(orderService);
    }
}