package com.ordertracker.order.controller;

import com.ordertracker.common.dto.PageResponse;
import com.ordertracker.order.dto.CreateOrderRequest;
import com.ordertracker.order.dto.OrderHistoryResponse;
import com.ordertracker.order.dto.OrderResponse;
import com.ordertracker.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/orders")
@Validated
@RequiredArgsConstructor
@Tag(
        name = "Orders",
        description = "Order management and status history"
)
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;

    @Operation(
            summary = "Create a new order",
            description = "Creates a new order for the authenticated user"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Order created successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid order request"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required"
            )
    })
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid
            @RequestBody
            CreateOrderRequest request,

            @Parameter(hidden = true)
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

    @Operation(
            summary = "Get current user's orders",
            description = "Returns a paginated list of orders owned by the authenticated user"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Orders retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid pagination parameters"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required"
            )
    })
    @GetMapping
    public ResponseEntity<PageResponse<OrderResponse>>
    getMyOrders(
            @Parameter(hidden = true)
            Principal principal,

            @Parameter(
                    description = "Zero-based page index",
                    example = "0"
            )
            @RequestParam(defaultValue = "0")
            @Min(0)
            int page,

            @Parameter(
                    description = "Number of items per page, between 1 and 100",
                    example = "20"
            )
            @RequestParam(defaultValue = "20")
            @Min(1)
            @Max(100)
            int size
    ) {
        return ResponseEntity.ok(
                orderService.getMyOrders(
                        principal.getName(),
                        page,
                        size
                )
        );
    }

    @Operation(
            summary = "Get order by ID",
            description = "Returns an order when the authenticated user is allowed to access it"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Order retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access to the order is forbidden"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Order not found"
            )
    })
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(
            @Parameter(
                    description = "Order identifier",
                    example = "10"
            )
            @PathVariable
            Long orderId,

            @Parameter(hidden = true)
            Principal principal
    ) {
        return ResponseEntity.ok(
                orderService.getOrder(
                        orderId,
                        principal.getName()
                )
        );
    }

    @Operation(
            summary = "Cancel an order",
            description = "Cancels an order when its current status allows cancellation"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Order cancelled successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Order cannot be cancelled from its current status"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access to the order is forbidden"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Order not found"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Order was modified concurrently"
            )
    })
    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @Parameter(
                    description = "Order identifier",
                    example = "10"
            )
            @PathVariable
            Long orderId,

            @Parameter(hidden = true)
            Principal principal
    ) {
        return ResponseEntity.ok(
                orderService.cancelOrder(
                        orderId,
                        principal.getName()
                )
        );
    }

    @Operation(
            summary = "Get order status history",
            description = "Returns the paginated status change history of an accessible order"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Order history retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid pagination parameters"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access to the order is forbidden"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Order not found"
            )
    })
    @GetMapping("/{orderId}/history")
    public ResponseEntity<PageResponse<OrderHistoryResponse>>
    getOrderHistory(
            @Parameter(
                    description = "Order identifier",
                    example = "10"
            )
            @PathVariable
            Long orderId,

            @Parameter(hidden = true)
            Principal principal,

            @Parameter(
                    description = "Zero-based page index",
                    example = "0"
            )
            @RequestParam(defaultValue = "0")
            @Min(0)
            int page,

            @Parameter(
                    description = "Number of items per page, between 1 and 100",
                    example = "20"
            )
            @RequestParam(defaultValue = "20")
            @Min(1)
            @Max(100)
            int size
    ) {
        return ResponseEntity.ok(
                orderService.getOrderHistory(
                        orderId,
                        principal.getName(),
                        page,
                        size
                )
        );
    }
}