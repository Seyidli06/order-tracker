package com.ordertracker.order.controller;

import com.ordertracker.common.enums.OrderStatus;
import com.ordertracker.common.enums.StatusChangeSource;
import com.ordertracker.order.dto.CreateOrderRequest;
import com.ordertracker.order.dto.OrderHistoryResponse;
import com.ordertracker.order.dto.OrderResponse;
import com.ordertracker.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private Principal principal;

    private OrderController orderController;

    @BeforeEach
    void setUp() {
        orderController = new OrderController(orderService);

        when(principal.getName())
                .thenReturn("user@test.com");
    }

    @Test
    void shouldCreateOrder() {
        CreateOrderRequest request =
                new CreateOrderRequest(
                        new BigDecimal("149.99"),
                        "AZN"
                );

        OrderResponse serviceResponse =
                createOrderResponse();

        when(
                orderService.createOrder(
                        "user@test.com",
                        request
                )
        ).thenReturn(serviceResponse);

        ResponseEntity<OrderResponse> response =
                orderController.createOrder(
                        request,
                        principal
                );

        assertEquals(
                HttpStatus.CREATED,
                response.getStatusCode()
        );

        assertEquals(
                serviceResponse,
                response.getBody()
        );

        verify(orderService)
                .createOrder(
                        "user@test.com",
                        request
                );
    }

    @Test
    void shouldReturnCurrentUsersOrders() {
        List<OrderResponse> orders =
                List.of(
                        createOrderResponse()
                );

        when(
                orderService.getMyOrders(
                        "user@test.com"
                )
        ).thenReturn(orders);

        ResponseEntity<List<OrderResponse>> response =
                orderController.getMyOrders(
                        principal
                );

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode()
        );

        assertEquals(
                orders,
                response.getBody()
        );

        verify(orderService)
                .getMyOrders(
                        "user@test.com"
                );
    }

    @Test
    void shouldReturnOrderById() {
        OrderResponse serviceResponse =
                createOrderResponse();

        when(
                orderService.getOrder(
                        10L,
                        "user@test.com"
                )
        ).thenReturn(serviceResponse);

        ResponseEntity<OrderResponse> response =
                orderController.getOrder(
                        10L,
                        principal
                );

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode()
        );

        assertEquals(
                serviceResponse,
                response.getBody()
        );

        verify(orderService)
                .getOrder(
                        10L,
                        "user@test.com"
                );
    }

    @Test
    void shouldReturnOrderHistory() {
        OrderHistoryResponse history =
                new OrderHistoryResponse(
                        100L,
                        OrderStatus.PAYMENT_PENDING,
                        OrderStatus.PAID,
                        StatusChangeSource.PAYMENT_WEBHOOK,
                        "pay_evt_123",
                        Instant.parse(
                                "2026-08-26T10:00:00Z"
                        )
                );

        List<OrderHistoryResponse> historyList =
                List.of(history);

        when(
                orderService.getOrderHistory(
                        10L,
                        "user@test.com"
                )
        ).thenReturn(historyList);

        ResponseEntity<List<OrderHistoryResponse>> response =
                orderController.getOrderHistory(
                        10L,
                        principal
                );

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode()
        );

        assertEquals(
                historyList,
                response.getBody()
        );

        verify(orderService)
                .getOrderHistory(
                        10L,
                        "user@test.com"
                );
    }

    private OrderResponse createOrderResponse() {
        return new OrderResponse(
                10L,
                "ord_test_123",
                1L,
                new BigDecimal("149.99"),
                "AZN",
                OrderStatus.CREATED,
                Instant.parse(
                        "2026-08-26T10:00:00Z"
                ),
                Instant.parse(
                        "2026-08-26T10:00:00Z"
                )
        );
    }

    @Test
    void shouldCancelOrder() {
        OrderResponse cancelledOrder =
                new OrderResponse(
                        10L,
                        "ord_test_123",
                        1L,
                        new BigDecimal("149.99"),
                        "AZN",
                        OrderStatus.CANCELLED,
                        Instant.parse(
                                "2026-08-26T10:00:00Z"
                        ),
                        Instant.parse(
                                "2026-08-26T10:05:00Z"
                        )
                );

        when(
                orderService.cancelOrder(
                        10L,
                        "user@test.com"
                )
        ).thenReturn(cancelledOrder);

        ResponseEntity<OrderResponse> response =
                orderController.cancelOrder(
                        10L,
                        principal
                );

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode()
        );

        assertEquals(
                OrderStatus.CANCELLED,
                response.getBody().status()
        );

        verify(orderService)
                .cancelOrder(
                        10L,
                        "user@test.com"
                );
    }
}