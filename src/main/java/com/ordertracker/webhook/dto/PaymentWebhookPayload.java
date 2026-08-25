package com.ordertracker.webhook.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentWebhookPayload {

    @NotBlank(message = "Event ID is required")
    @JsonProperty("event_id")
    private String eventId;

    @NotBlank(message = "Event type is required")
    @JsonProperty("event_type")
    private String eventType;

    @NotBlank(message = "Source is required")
    private String source;

    @NotNull(message = "Timestamp is required")
    @JsonProperty("timestamp")
    private Instant timestamp;

    @NotNull(message = "Payment data is required")
    @JsonProperty("payment_data")
    private PaymentData paymentData;

    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentData {

        @NotBlank(message = "Payment ID is required")
        @JsonProperty("payment_id")
        private String paymentId;

        @NotBlank(message = "Order ID is required")
        @JsonProperty("order_id")
        private String orderId;

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be positive")
        private BigDecimal amount;

        @NotBlank(message = "Currency is required")
        private String currency;

        @NotBlank(message = "Status is required")
        private String status;

        @JsonProperty("transaction_id")
        private String transactionId;

        @JsonProperty("payment_method")
        private String paymentMethod;
    }
}
