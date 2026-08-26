package com.ordertracker.order.mapper;

import com.ordertracker.order.dto.OrderHistoryResponse;
import com.ordertracker.order.dto.OrderResponse;
import com.ordertracker.order.entity.Order;
import com.ordertracker.order.entity.OrderStatusHistory;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

    public OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getExternalOrderId(),
                order.getUser().getId(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    public OrderHistoryResponse toHistoryResponse(
            OrderStatusHistory history
    ) {
        return new OrderHistoryResponse(
                history.getId(),
                history.getPreviousStatus(),
                history.getNewStatus(),
                history.getSource(),
                history.getReferenceId(),
                history.getChangedAt()
        );
    }
}