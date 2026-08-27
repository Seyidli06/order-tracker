package com.ordertracker.order.service;

import com.ordertracker.common.enums.OrderStatus;
import com.ordertracker.common.enums.Role;
import com.ordertracker.common.enums.StatusChangeSource;
import com.ordertracker.order.dto.CreateOrderRequest;
import com.ordertracker.order.dto.OrderHistoryResponse;
import com.ordertracker.order.dto.OrderResponse;
import com.ordertracker.order.entity.Order;
import com.ordertracker.order.entity.OrderStatusHistory;
import com.ordertracker.order.mapper.OrderMapper;
import com.ordertracker.order.repository.OrderRepository;
import com.ordertracker.order.repository.OrderStatusHistoryRepository;
import com.ordertracker.user.entity.User;
import com.ordertracker.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;

    private final OrderStatusHistoryRepository
            orderStatusHistoryRepository;

    private final UserRepository userRepository;

    private final OrderMapper orderMapper;

    private final OrderStatusTransitionValidator
            orderStatusTransitionValidator;

    public OrderResponse createOrder(
            String email,
            CreateOrderRequest request
    ) {
        User user = getUserByEmail(email);

        Order order = Order.builder()
                .externalOrderId(
                        generateExternalOrderId()
                )
                .user(user)
                .totalAmount(
                        request.totalAmount()
                )
                .currency(
                        request.currency()
                                .toUpperCase(
                                        Locale.ROOT
                                )
                )
                .status(
                        OrderStatus.CREATED
                )
                .build();

        Order savedOrder =
                orderRepository.save(order);

        saveHistory(
                savedOrder,
                null,
                OrderStatus.CREATED,
                StatusChangeSource.USER,
                null
        );

        return orderMapper.toResponse(
                savedOrder
        );
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(
            String email
    ) {
        User user =
                getUserByEmail(email);

        return orderRepository
                .findAllByUserIdOrderByCreatedAtDesc(
                        user.getId()
                )
                .stream()
                .map(
                        orderMapper::toResponse
                )
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(
            Long orderId,
            String email
    ) {
        User currentUser =
                getUserByEmail(email);

        Order order =
                getOrderById(orderId);

        validateOrderAccess(
                order,
                currentUser
        );

        return orderMapper.toResponse(
                order
        );
    }

    @Transactional(readOnly = true)
    public List<OrderHistoryResponse> getOrderHistory(
            Long orderId,
            String email
    ) {
        User currentUser =
                getUserByEmail(email);

        Order order =
                getOrderById(orderId);

        validateOrderAccess(
                order,
                currentUser
        );

        return orderStatusHistoryRepository
                .findAllByOrderIdOrderByChangedAtDesc(
                        order.getId()
                )
                .stream()
                .map(
                        orderMapper::toHistoryResponse
                )
                .toList();
    }

    public OrderResponse updateStatus(
            String externalOrderId,
            OrderStatus newStatus,
            StatusChangeSource source,
            String referenceId
    ) {
        if (newStatus == null) {
            throw new IllegalArgumentException(
                    "New order status must not be null"
            );
        }

        if (source == null) {
            throw new IllegalArgumentException(
                    "Status change source must not be null"
            );
        }

        Order order =
                orderRepository
                        .findByExternalOrderId(
                                externalOrderId
                        )
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                "Order not found: "
                                                        + externalOrderId
                                        )
                        );

        OrderStatus previousStatus =
                order.getStatus();

        /*
         * Idempotency:
         *
         * Eyni webhook/event statusu təkrar göndərilərsə
         * order yenidən save edilmir və lazımsız history
         * yaradılmır.
         */
        if (previousStatus == newStatus) {
            return orderMapper.toResponse(
                    order
            );
        }

        /*
         * State transition validation:
         *
         * Məsələn:
         *
         * PAYMENT_PENDING -> PAID       allowed
         * PAID -> SHIPPED               allowed
         * SHIPPED -> DELIVERED          allowed
         *
         * DELIVERED -> PAYMENT_PENDING  rejected
         */
        orderStatusTransitionValidator
                .validate(
                        previousStatus,
                        newStatus
                );

        order.setStatus(
                newStatus
        );

        Order savedOrder =
                orderRepository.save(
                        order
                );

        saveHistory(
                savedOrder,
                previousStatus,
                newStatus,
                source,
                referenceId
        );

        return orderMapper.toResponse(
                savedOrder
        );
    }

    private void saveHistory(
            Order order,
            OrderStatus previousStatus,
            OrderStatus newStatus,
            StatusChangeSource source,
            String referenceId
    ) {
        OrderStatusHistory history =
                OrderStatusHistory.builder()
                        .order(
                                order
                        )
                        .previousStatus(
                                previousStatus
                        )
                        .newStatus(
                                newStatus
                        )
                        .source(
                                source
                        )
                        .referenceId(
                                referenceId
                        )
                        .build();

        orderStatusHistoryRepository
                .save(
                        history
                );
    }

    private User getUserByEmail(
            String email
    ) {
        return userRepository
                .findByEmail(
                        email
                )
                .orElseThrow(
                        () ->
                                new EntityNotFoundException(
                                        "User not found: "
                                                + email
                                )
                );
    }

    private Order getOrderById(
            Long orderId
    ) {
        return orderRepository
                .findById(
                        orderId
                )
                .orElseThrow(
                        () ->
                                new EntityNotFoundException(
                                        "Order not found: "
                                                + orderId
                                )
                );
    }

    private void validateOrderAccess(
            Order order,
            User currentUser
    ) {
        if (currentUser.getRole()
                == Role.ADMIN) {

            return;
        }

        if (!order.getUser()
                .getId()
                .equals(
                        currentUser.getId()
                )) {

            throw new AccessDeniedException(
                    "You are not allowed to access this order"
            );
        }
    }

    private String generateExternalOrderId() {
        String externalOrderId;

        do {
            externalOrderId =
                    "ord_"
                            + UUID.randomUUID()
                            .toString()
                            .replace(
                                    "-",
                                    ""
                            );

        } while (
                orderRepository
                        .existsByExternalOrderId(
                                externalOrderId
                        )
        );

        return externalOrderId;
    }
}