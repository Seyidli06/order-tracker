package com.ordertracker.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record CreateOrderRequest(

        @NotNull
        @DecimalMin(
                value = "0.01",
                message = "Total amount must be at least 0.01"
        )
        BigDecimal totalAmount,

        @NotBlank
        @Pattern(
                regexp = "^[A-Za-z]{3}$",
                message = "Currency must be a 3-letter code"
        )
        String currency

) {
}