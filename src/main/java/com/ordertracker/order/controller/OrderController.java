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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(
        name = "Orders",
        description = "Order management and status history"
)
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;


    @Operation(summary = "Create a new order")
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

    @Operation(summary = "Get current user's orders")
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

    @Operation(summary = "Get order by ID")
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

    @Operation(summary = "Cancel an order")
    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable Long orderId,
            Principal principal
    ) {
        return ResponseEntity.ok(
                orderService.cancelOrder(
                        orderId,
                        principal.getName()
                )
        );
    }

    @Operation(summary = "Get order status history")
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