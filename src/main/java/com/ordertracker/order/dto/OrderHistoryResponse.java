package com.ordertracker.order.dto;

import com.ordertracker.common.enums.OrderStatus;
import com.ordertracker.common.enums.StatusChangeSource;

import java.time.Instant;

public record OrderHistoryResponse(

        Long id,

        OrderStatus previousStatus,

        OrderStatus newStatus,

        StatusChangeSource source,

        String referenceId,

        Instant changedAt

) {
}