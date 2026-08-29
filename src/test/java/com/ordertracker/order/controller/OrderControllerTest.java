package com.ordertracker.order.controller;

import com.ordertracker.common.dto.PageResponse;
import com.ordertracker.common.enums.OrderStatus;
import com.ordertracker.common.enums.StatusChangeSource;
import com.ordertracker.order.dto.CreateOrderRequest;
import com.ordertracker.order.dto.OrderHistoryResponse;
import com.ordertracker.order.dto.OrderResponse;
import com.ordertracker.order.service.OrderService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.executable.ExecutableValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        orderController =
                new OrderController(
                        orderService
                );
    }

    @Test
    void shouldCreateOrder() {
        mockAuthenticatedPrincipal();

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
        ).thenReturn(
                serviceResponse
        );

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

        verify(
                orderService
        ).createOrder(
                "user@test.com",
                request
        );
    }

    @Test
    void shouldReturnCurrentUsersOrders() {
        mockAuthenticatedPrincipal();

        List<OrderResponse> orders =
                List.of(
                        createOrderResponse()
                );

        PageResponse<OrderResponse> pageResponse =
                new PageResponse<>(
                        orders,
                        0,
                        20,
                        1,
                        1,
                        true,
                        true
                );

        when(
                orderService.getMyOrders(
                        "user@test.com",
                        0,
                        20
                )
        ).thenReturn(
                pageResponse
        );

        ResponseEntity<PageResponse<OrderResponse>>
                response =
                orderController.getMyOrders(
                        principal,
                        0,
                        20
                );

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode()
        );

        assertEquals(
                pageResponse,
                response.getBody()
        );

        assertEquals(
                1,
                response.getBody()
                        .content()
                        .size()
        );

        verify(
                orderService
        ).getMyOrders(
                "user@test.com",
                0,
                20
        );
    }

    @Test
    void shouldReturnOrderById() {
        mockAuthenticatedPrincipal();

        OrderResponse serviceResponse =
                createOrderResponse();

        when(
                orderService.getOrder(
                        10L,
                        "user@test.com"
                )
        ).thenReturn(
                serviceResponse
        );

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

        verify(
                orderService
        ).getOrder(
                10L,
                "user@test.com"
        );
    }

    @Test
    void shouldReturnOrderHistory() {
        mockAuthenticatedPrincipal();

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

        PageResponse<OrderHistoryResponse>
                pageResponse =
                new PageResponse<>(
                        List.of(history),
                        0,
                        20,
                        1,
                        1,
                        true,
                        true
                );

        when(
                orderService.getOrderHistory(
                        10L,
                        "user@test.com",
                        0,
                        20
                )
        ).thenReturn(
                pageResponse
        );

        ResponseEntity<PageResponse<OrderHistoryResponse>>
                response =
                orderController.getOrderHistory(
                        10L,
                        principal,
                        0,
                        20
                );

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode()
        );

        assertEquals(
                pageResponse,
                response.getBody()
        );

        verify(
                orderService
        ).getOrderHistory(
                10L,
                "user@test.com",
                0,
                20
        );
    }

    @Test
    void shouldCancelOrder() {
        mockAuthenticatedPrincipal();

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
        ).thenReturn(
                cancelledOrder
        );

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
                response.getBody()
                        .status()
        );

        verify(
                orderService
        ).cancelOrder(
                10L,
                "user@test.com"
        );
    }

    @Test
    void shouldRejectNegativePage()
            throws Exception {

        Set<ConstraintViolation<OrderController>>
                violations =
                validateGetMyOrdersParameters(
                        -1,
                        20
                );

        assertFalse(
                violations.isEmpty()
        );
    }

    @Test
    void shouldRejectZeroPageSize()
            throws Exception {

        Set<ConstraintViolation<OrderController>>
                violations =
                validateGetMyOrdersParameters(
                        0,
                        0
                );

        assertFalse(
                violations.isEmpty()
        );
    }

    @Test
    void shouldRejectPageSizeAboveMaximum()
            throws Exception {

        Set<ConstraintViolation<OrderController>>
                violations =
                validateGetMyOrdersParameters(
                        0,
                        101
                );

        assertFalse(
                violations.isEmpty()
        );
    }

    private void mockAuthenticatedPrincipal() {
        when(
                principal.getName()
        ).thenReturn(
                "user@test.com"
        );
    }

    private Set<ConstraintViolation<OrderController>>
    validateGetMyOrdersParameters(
            int page,
            int size
    ) throws Exception {

        Method method =
                OrderController.class.getMethod(
                        "getMyOrders",
                        Principal.class,
                        int.class,
                        int.class
                );

        try (
                ValidatorFactory validatorFactory =
                        Validation
                                .buildDefaultValidatorFactory()
        ) {
            ExecutableValidator executableValidator =
                    validatorFactory
                            .getValidator()
                            .forExecutables();

            return executableValidator
                    .validateParameters(
                            orderController,
                            method,
                            new Object[]{
                                    principal,
                                    page,
                                    size
                            }
                    );
        }
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
}