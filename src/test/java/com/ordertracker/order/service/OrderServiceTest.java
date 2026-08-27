package com.ordertracker.order.service;

import com.ordertracker.common.enums.OrderStatus;
import com.ordertracker.common.enums.Role;
import com.ordertracker.common.enums.StatusChangeSource;
import com.ordertracker.exception.ResourceConflictException;
import com.ordertracker.order.dto.CreateOrderRequest;
import com.ordertracker.order.dto.OrderResponse;
import com.ordertracker.order.entity.Order;
import com.ordertracker.order.entity.OrderStatusHistory;
import com.ordertracker.order.mapper.OrderMapper;
import com.ordertracker.order.repository.OrderRepository;
import com.ordertracker.order.repository.OrderStatusHistoryRepository;
import com.ordertracker.user.entity.User;
import com.ordertracker.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderStatusHistoryRepository
            orderStatusHistoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderStatusTransitionValidator
            orderStatusTransitionValidator;

    private OrderService orderService;

    private User regularUser;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(
                orderRepository,
                orderStatusHistoryRepository,
                userRepository,
                new OrderMapper(),
                orderStatusTransitionValidator
        );

        regularUser = User.builder()
                .id(1L)
                .email("user@test.com")
                .password("encoded-password")
                .role(Role.USER)
                .build();
    }

    @Test
    void shouldCreateOrderAndInitialHistory() {
        CreateOrderRequest request =
                new CreateOrderRequest(
                        new BigDecimal("149.99"),
                        "azn"
                );

        when(
                userRepository.findByEmail(
                        "user@test.com"
                )
        ).thenReturn(
                Optional.of(regularUser)
        );

        when(
                orderRepository.save(
                        any(Order.class)
                )
        ).thenAnswer(
                invocation -> {
                    Order order =
                            invocation.getArgument(0);

                    order.setId(10L);

                    return order;
                }
        );

        OrderResponse response =
                orderService.createOrder(
                        "user@test.com",
                        request
                );

        assertEquals(
                10L,
                response.id()
        );

        assertEquals(
                1L,
                response.userId()
        );

        assertEquals(
                new BigDecimal("149.99"),
                response.totalAmount()
        );

        assertEquals(
                "AZN",
                response.currency()
        );

        assertEquals(
                OrderStatus.CREATED,
                response.status()
        );

        assertNotNull(
                response.externalOrderId()
        );

        assertTrue(
                response.externalOrderId()
                        .startsWith("ord_")
        );

        ArgumentCaptor<Order> orderCaptor =
                ArgumentCaptor.forClass(
                        Order.class
                );

        verify(
                orderRepository
        ).save(
                orderCaptor.capture()
        );

        Order savedOrder =
                orderCaptor.getValue();

        assertEquals(
                regularUser,
                savedOrder.getUser()
        );

        assertEquals(
                OrderStatus.CREATED,
                savedOrder.getStatus()
        );

        assertEquals(
                "AZN",
                savedOrder.getCurrency()
        );

        ArgumentCaptor<OrderStatusHistory>
                historyCaptor =
                ArgumentCaptor.forClass(
                        OrderStatusHistory.class
                );

        verify(
                orderStatusHistoryRepository
        ).save(
                historyCaptor.capture()
        );

        OrderStatusHistory history =
                historyCaptor.getValue();

        assertEquals(
                savedOrder,
                history.getOrder()
        );

        assertNull(
                history.getPreviousStatus()
        );

        assertEquals(
                OrderStatus.CREATED,
                history.getNewStatus()
        );

        assertEquals(
                StatusChangeSource.USER,
                history.getSource()
        );

        assertNull(
                history.getReferenceId()
        );

        verifyNoInteractions(
                orderStatusTransitionValidator
        );
    }

    @Test
    void shouldDenyUserFromAccessingAnotherUsersOrder() {
        User owner =
                regularUser;

        User anotherUser =
                User.builder()
                        .id(2L)
                        .email(
                                "another@test.com"
                        )
                        .password(
                                "encoded-password"
                        )
                        .role(Role.USER)
                        .build();

        Order order =
                createOrder(
                        10L,
                        "ord_test_1",
                        owner,
                        OrderStatus.CREATED
                );

        when(
                userRepository.findByEmail(
                        "another@test.com"
                )
        ).thenReturn(
                Optional.of(anotherUser)
        );

        when(
                orderRepository.findById(
                        10L
                )
        ).thenReturn(
                Optional.of(order)
        );

        assertThrows(
                AccessDeniedException.class,
                () ->
                        orderService.getOrder(
                                10L,
                                "another@test.com"
                        )
        );

        verifyNoInteractions(
                orderStatusTransitionValidator
        );
    }

    @Test
    void shouldAllowAdminToAccessAnyOrder() {
        User admin =
                User.builder()
                        .id(99L)
                        .email(
                                "admin@test.com"
                        )
                        .password(
                                "encoded-password"
                        )
                        .role(Role.ADMIN)
                        .build();

        Order order =
                createOrder(
                        10L,
                        "ord_test_1",
                        regularUser,
                        OrderStatus.CREATED
                );

        when(
                userRepository.findByEmail(
                        "admin@test.com"
                )
        ).thenReturn(
                Optional.of(admin)
        );

        when(
                orderRepository.findById(
                        10L
                )
        ).thenReturn(
                Optional.of(order)
        );

        OrderResponse response =
                orderService.getOrder(
                        10L,
                        "admin@test.com"
                );

        assertEquals(
                10L,
                response.id()
        );

        assertEquals(
                regularUser.getId(),
                response.userId()
        );

        verifyNoInteractions(
                orderStatusTransitionValidator
        );
    }

    @Test
    void shouldUpdateOrderStatusAndCreateHistory() {
        Order order =
                createOrder(
                        10L,
                        "ord_test_1",
                        regularUser,
                        OrderStatus.PAYMENT_PENDING
                );

        when(
                orderRepository
                        .findByExternalOrderId(
                                "ord_test_1"
                        )
        ).thenReturn(
                Optional.of(order)
        );

        when(
                orderRepository.save(order)
        ).thenReturn(order);

        OrderResponse response =
                orderService.updateStatus(
                        "ord_test_1",
                        OrderStatus.PAID,
                        StatusChangeSource.PAYMENT_WEBHOOK,
                        "pay_evt_123"
                );

        assertEquals(
                OrderStatus.PAID,
                response.status()
        );

        assertEquals(
                OrderStatus.PAID,
                order.getStatus()
        );

        verify(
                orderStatusTransitionValidator
        ).validate(
                OrderStatus.PAYMENT_PENDING,
                OrderStatus.PAID
        );

        verify(
                orderRepository
        ).save(
                order
        );

        ArgumentCaptor<OrderStatusHistory>
                historyCaptor =
                ArgumentCaptor.forClass(
                        OrderStatusHistory.class
                );

        verify(
                orderStatusHistoryRepository
        ).save(
                historyCaptor.capture()
        );

        OrderStatusHistory history =
                historyCaptor.getValue();

        assertEquals(
                OrderStatus.PAYMENT_PENDING,
                history.getPreviousStatus()
        );

        assertEquals(
                OrderStatus.PAID,
                history.getNewStatus()
        );

        assertEquals(
                StatusChangeSource.PAYMENT_WEBHOOK,
                history.getSource()
        );

        assertEquals(
                "pay_evt_123",
                history.getReferenceId()
        );
    }

    @Test
    void shouldNotCreateHistoryWhenStatusDoesNotChange() {
        Order order =
                createOrder(
                        10L,
                        "ord_test_1",
                        regularUser,
                        OrderStatus.PAID
                );

        when(
                orderRepository
                        .findByExternalOrderId(
                                "ord_test_1"
                        )
        ).thenReturn(
                Optional.of(order)
        );

        OrderResponse response =
                orderService.updateStatus(
                        "ord_test_1",
                        OrderStatus.PAID,
                        StatusChangeSource.PAYMENT_WEBHOOK,
                        "pay_evt_duplicate"
                );

        assertEquals(
                OrderStatus.PAID,
                response.status()
        );

        verify(
                orderStatusTransitionValidator,
                never()
        ).validate(
                any(OrderStatus.class),
                any(OrderStatus.class)
        );

        verify(
                orderRepository,
                never()
        ).save(
                any(Order.class)
        );

        verify(
                orderStatusHistoryRepository,
                never()
        ).save(
                any(OrderStatusHistory.class)
        );
    }

    @Test
    void shouldRejectInvalidStatusTransition() {
        Order order =
                createOrder(
                        10L,
                        "ord_test_1",
                        regularUser,
                        OrderStatus.DELIVERED
                );

        when(
                orderRepository
                        .findByExternalOrderId(
                                "ord_test_1"
                        )
        ).thenReturn(
                Optional.of(order)
        );

        doThrow(
                new ResourceConflictException(
                        "Invalid order status transition: "
                                + "DELIVERED -> PAYMENT_PENDING"
                )
        ).when(
                orderStatusTransitionValidator
        ).validate(
                OrderStatus.DELIVERED,
                OrderStatus.PAYMENT_PENDING
        );

        ResourceConflictException exception =
                assertThrows(
                        ResourceConflictException.class,
                        () ->
                                orderService.updateStatus(
                                        "ord_test_1",
                                        OrderStatus.PAYMENT_PENDING,
                                        StatusChangeSource.SYSTEM,
                                        null
                                )
                );

        assertEquals(
                "Invalid order status transition: "
                        + "DELIVERED -> PAYMENT_PENDING",
                exception.getMessage()
        );

        /*
         * Validator exception atdığı üçün
         * order dəyişməməlidir.
         */
        assertEquals(
                OrderStatus.DELIVERED,
                order.getStatus()
        );

        verify(
                orderStatusTransitionValidator
        ).validate(
                OrderStatus.DELIVERED,
                OrderStatus.PAYMENT_PENDING
        );

        verify(
                orderRepository,
                never()
        ).save(
                any(Order.class)
        );

        verify(
                orderStatusHistoryRepository,
                never()
        ).save(
                any(OrderStatusHistory.class)
        );
    }

    @Test
    void shouldThrowExceptionWhenOrderDoesNotExist() {
        when(
                orderRepository
                        .findByExternalOrderId(
                                "missing-order"
                        )
        ).thenReturn(
                Optional.empty()
        );

        EntityNotFoundException exception =
                assertThrows(
                        EntityNotFoundException.class,
                        () ->
                                orderService.updateStatus(
                                        "missing-order",
                                        OrderStatus.PAID,
                                        StatusChangeSource.PAYMENT_WEBHOOK,
                                        "pay_evt_404"
                                )
                );

        assertEquals(
                "Order not found: missing-order",
                exception.getMessage()
        );

        verify(
                orderStatusTransitionValidator,
                never()
        ).validate(
                any(OrderStatus.class),
                any(OrderStatus.class)
        );

        verify(
                orderRepository,
                never()
        ).save(
                any(Order.class)
        );

        verify(
                orderStatusHistoryRepository,
                never()
        ).save(
                any(OrderStatusHistory.class)
        );
    }

    private Order createOrder(
            Long id,
            String externalOrderId,
            User user,
            OrderStatus status
    ) {
        return Order.builder()
                .id(id)
                .externalOrderId(
                        externalOrderId
                )
                .user(user)
                .totalAmount(
                        new BigDecimal(
                                "100.00"
                        )
                )
                .currency("AZN")
                .status(status)
                .build();
    }
}