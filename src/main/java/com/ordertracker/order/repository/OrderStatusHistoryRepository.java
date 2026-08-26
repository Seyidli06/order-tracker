package com.ordertracker.order.repository;

import com.ordertracker.order.entity.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderStatusHistoryRepository
        extends JpaRepository<OrderStatusHistory, Long> {

    List<OrderStatusHistory>
    findAllByOrderIdOrderByChangedAtDesc(Long orderId);
}