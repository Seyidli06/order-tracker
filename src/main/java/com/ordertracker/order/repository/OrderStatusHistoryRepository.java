package com.ordertracker.order.repository;

import com.ordertracker.order.entity.OrderStatusHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderStatusHistoryRepository
        extends JpaRepository<OrderStatusHistory, Long> {

    Page<OrderStatusHistory>
    findAllByOrderIdOrderByChangedAtDesc(
            Long orderId,
            Pageable pageable
    );
}