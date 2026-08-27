package com.ordertracker.order.service;

import com.ordertracker.common.enums.OrderStatus;
import com.ordertracker.exception.ResourceConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderStatusTransitionValidatorTest {

    private OrderStatusTransitionValidator validator;

    @BeforeEach
    void setUp() {
        validator =
                new OrderStatusTransitionValidator();
    }

    @Test
    void shouldAllowCreatedToPaymentPending() {
        assertDoesNotThrow(
                () -> validator.validate(
                        OrderStatus.CREATED,
                        OrderStatus.PAYMENT_PENDING
                )
        );
    }

    @Test
    void shouldAllowPaymentPendingToPaid() {
        assertDoesNotThrow(
                () -> validator.validate(
                        OrderStatus.PAYMENT_PENDING,
                        OrderStatus.PAID
                )
        );
    }

    @Test
    void shouldAllowPaidToShipped() {
        assertDoesNotThrow(
                () -> validator.validate(
                        OrderStatus.PAID,
                        OrderStatus.SHIPPED
                )
        );
    }

    @Test
    void shouldAllowShippedToDelivered() {
        assertDoesNotThrow(
                () -> validator.validate(
                        OrderStatus.SHIPPED,
                        OrderStatus.DELIVERED
                )
        );
    }

    @Test
    void shouldAllowDeliveredToReturned() {
        assertDoesNotThrow(
                () -> validator.validate(
                        OrderStatus.DELIVERED,
                        OrderStatus.RETURNED
                )
        );
    }

    @Test
    void shouldAllowReturnedToRefunded() {
        assertDoesNotThrow(
                () -> validator.validate(
                        OrderStatus.RETURNED,
                        OrderStatus.REFUNDED
                )
        );
    }

    @Test
    void shouldAllowSameStatus() {
        assertDoesNotThrow(
                () -> validator.validate(
                        OrderStatus.PAID,
                        OrderStatus.PAID
                )
        );
    }

    @Test
    void shouldRejectDeliveredToPaymentPending() {
        ResourceConflictException exception =
                assertThrows(
                        ResourceConflictException.class,
                        () -> validator.validate(
                                OrderStatus.DELIVERED,
                                OrderStatus.PAYMENT_PENDING
                        )
                );

        assertEquals(
                "Invalid order status transition: "
                        + "DELIVERED -> PAYMENT_PENDING",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectShippedToPaid() {
        assertThrows(
                ResourceConflictException.class,
                () -> validator.validate(
                        OrderStatus.SHIPPED,
                        OrderStatus.PAID
                )
        );
    }

    @Test
    void shouldRejectTransitionFromRefunded() {
        assertThrows(
                ResourceConflictException.class,
                () -> validator.validate(
                        OrderStatus.REFUNDED,
                        OrderStatus.PAID
                )
        );
    }

    @Test
    void shouldRejectNullCurrentStatus() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> validator.validate(
                                null,
                                OrderStatus.PAID
                        )
                );

        assertEquals(
                "Current order status must not be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNullNewStatus() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> validator.validate(
                                OrderStatus.CREATED,
                                null
                        )
                );

        assertEquals(
                "New order status must not be null",
                exception.getMessage()
        );
    }
}