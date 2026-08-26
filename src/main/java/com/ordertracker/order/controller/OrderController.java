package com.ordertracker.order.controller;

import com.ordertracker.order.dto.CreateOrderRequest;
import com.ordertracker.order.dto.OrderHistoryResponse;
import com.ordertracker.order.dto.OrderResponse;
import com.ordertracker.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            Principal principal
    ) {
        OrderResponse response =
                orderService.createOrder(
                        principal.getName(),
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getMyOrders(
            Principal principal
    ) {
        return ResponseEntity.ok(
                orderService.getMyOrders(
                        principal.getName()
                )
        );
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(
            @PathVariable Long orderId,
            Principal principal
    ) {
        return ResponseEntity.ok(
                orderService.getOrder(
                        orderId,
                        principal.getName()
                )
        );
    }

    @GetMapping("/{orderId}/history")
    public ResponseEntity<List<OrderHistoryResponse>> getOrderHistory(
            @PathVariable Long orderId,
            Principal principal
    ) {
        return ResponseEntity.ok(
                orderService.getOrderHistory(
                        orderId,
                        principal.getName()
                )
        );
    }
}