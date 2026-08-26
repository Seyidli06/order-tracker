package com.ordertracker.common.enums;

public enum OrderStatus {

    CREATED,

    PAYMENT_PENDING,
    PAID,
    PAYMENT_FAILED,

    PROCESSING,

    SHIPPED,
    OUT_FOR_DELIVERY,
    DELIVERED,

    CANCELLED,
    REFUNDED,
    RETURNED
}