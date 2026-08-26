package com.ordertracker.order.dto;

import com.ordertracker.common.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderResponse(

        Long id,

        String externalOrderId,

        Long userId,

        BigDecimal totalAmount,

        String currency,

        OrderStatus status,

        Instant createdAt,

        Instant updatedAt

) {
}