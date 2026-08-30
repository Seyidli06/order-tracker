package com.ordertracker.order.repository;

import com.ordertracker.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository
        extends JpaRepository<Order, Long> {

    Optional<Order> findByExternalOrderId(
            String externalOrderId
    );

    @EntityGraph(
            attributePaths = "user"
    )
    Optional<Order> findWithUserByExternalOrderId(
            String externalOrderId
    );

    boolean existsByExternalOrderId(
            String externalOrderId
    );

    Page<Order> findAllByUserIdOrderByCreatedAtDesc(
            Long userId,
            Pageable pageable
    );
}