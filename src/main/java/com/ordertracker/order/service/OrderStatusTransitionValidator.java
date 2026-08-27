package com.ordertracker.order.service;

import com.ordertracker.common.enums.OrderStatus;
import com.ordertracker.exception.ResourceConflictException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class OrderStatusTransitionValidator {

    private static final Map<OrderStatus, Set<OrderStatus>>
            ALLOWED_TRANSITIONS =
            new EnumMap<>(OrderStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(
                OrderStatus.CREATED,
                EnumSet.of(
                        OrderStatus.PAYMENT_PENDING,
                        OrderStatus.PAID,
                        OrderStatus.PAYMENT_FAILED,
                        OrderStatus.CANCELLED
                )
        );

        ALLOWED_TRANSITIONS.put(
                OrderStatus.PAYMENT_PENDING,
                EnumSet.of(
                        OrderStatus.PAID,
                        OrderStatus.PAYMENT_FAILED,
                        OrderStatus.CANCELLED
                )
        );

        ALLOWED_TRANSITIONS.put(
                OrderStatus.PAYMENT_FAILED,
                EnumSet.of(
                        OrderStatus.PAYMENT_PENDING,
                        OrderStatus.PAID,
                        OrderStatus.CANCELLED
                )
        );

        ALLOWED_TRANSITIONS.put(
                OrderStatus.PAID,
                EnumSet.of(
                        OrderStatus.PROCESSING,
                        OrderStatus.SHIPPED,
                        OrderStatus.CANCELLED,
                        OrderStatus.REFUNDED
                )
        );

        ALLOWED_TRANSITIONS.put(
                OrderStatus.PROCESSING,
                EnumSet.of(
                        OrderStatus.SHIPPED,
                        OrderStatus.CANCELLED,
                        OrderStatus.REFUNDED
                )
        );

        ALLOWED_TRANSITIONS.put(
                OrderStatus.SHIPPED,
                EnumSet.of(
                        OrderStatus.OUT_FOR_DELIVERY,
                        OrderStatus.DELIVERED,
                        OrderStatus.RETURNED
                )
        );

        ALLOWED_TRANSITIONS.put(
                OrderStatus.OUT_FOR_DELIVERY,
                EnumSet.of(
                        OrderStatus.DELIVERED,
                        OrderStatus.RETURNED
                )
        );

        ALLOWED_TRANSITIONS.put(
                OrderStatus.DELIVERED,
                EnumSet.of(
                        OrderStatus.RETURNED,
                        OrderStatus.REFUNDED
                )
        );

        ALLOWED_TRANSITIONS.put(
                OrderStatus.RETURNED,
                EnumSet.of(
                        OrderStatus.REFUNDED
                )
        );

        ALLOWED_TRANSITIONS.put(
                OrderStatus.CANCELLED,
                EnumSet.of(
                        OrderStatus.REFUNDED
                )
        );

        ALLOWED_TRANSITIONS.put(
                OrderStatus.REFUNDED,
                EnumSet.noneOf(OrderStatus.class)
        );
    }

    public void validate(
            OrderStatus currentStatus,
            OrderStatus newStatus
    ) {
        if (currentStatus == null) {
            throw new IllegalArgumentException(
                    "Current order status must not be null"
            );
        }

        if (newStatus == null) {
            throw new IllegalArgumentException(
                    "New order status must not be null"
            );
        }

        if (currentStatus == newStatus) {
            return;
        }

        Set<OrderStatus> allowedStatuses =
                ALLOWED_TRANSITIONS.getOrDefault(
                        currentStatus,
                        Set.of()
                );

        if (!allowedStatuses.contains(newStatus)) {
            throw new ResourceConflictException(
                    "Invalid order status transition: "
                            + currentStatus
                            + " -> "
                            + newStatus
            );
        }
    }
}