package com.ordertracker.webhook.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentWebhookPayload {

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

    @NotNull(message = "Shipment data is required")
    @Valid
    @JsonProperty("shipment_data")
    private ShipmentData shipmentData;

    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShipmentData {

        @NotBlank(message = "Shipment ID is required")
        @JsonProperty("shipment_id")
        private String shipmentId;

        @NotBlank(message = "Order ID is required")
        @JsonProperty("order_id")
        private String orderId;

        @NotBlank(message = "Tracking number is required")
        @JsonProperty("tracking_number")
        private String trackingNumber;

        @NotBlank(message = "Carrier is required")
        private String carrier;

        @NotBlank(message = "Status is required")
        private String status;

        @JsonProperty("origin_address")
        private Address originAddress;

        @JsonProperty("destination_address")
        private Address destinationAddress;

        @JsonProperty("estimated_delivery")
        private Instant estimatedDelivery;

        @JsonProperty("actual_delivery")
        private Instant actualDelivery;

        @NotNull(message = "Packages is required")
        private List<@Valid Package> packages;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Address {

        private String street;
        private String city;
        private String state;
        private String postalCode;
        private String country;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Package {

        @NotBlank(message = "Package ID is required")
        @JsonProperty("package_id")
        private String packageId;

        @Positive(message = "Weight must be positive")
        private Double weight;

        private String weightUnit;

        @JsonProperty("dimensions")
        private Dimensions dimensions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Dimensions {

        private Double length;
        private Double width;
        private Double height;
        private String unit;
    }
}