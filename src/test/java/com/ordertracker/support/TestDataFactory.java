package com.ordertracker.support;

import com.ordertracker.audit.WebhookAuditLog;
import com.ordertracker.notification.dto.EmailNotificationRequest;
import com.ordertracker.webhook.dto.PaymentWebhookPayload;
import com.ordertracker.webhook.dto.ShipmentWebhookPayload;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public class TestDataFactory {

    private TestDataFactory() {
    }

    public static WebhookAuditLog createWebhookAuditLog() {
        return WebhookAuditLog.builder()
                .id(1L)
                .eventId("evt_test_12345")
                .eventType("payment.completed")
                .source("stripe")
                .payload("{\"test\": \"payload\"}")
                .headers("{\"content-type\": \"application/json\"}")
                .receivedAt(Instant.now())
                .processingStatus(WebhookAuditLog.ProcessingStatus.PENDING)
                .retryCount(0)
                .build();
    }

    public static WebhookAuditLog createWebhookAuditLog(WebhookAuditLog.ProcessingStatus status) {
        return WebhookAuditLog.builder()
                .id(1L)
                .eventId("evt_test_12345")
                .eventType("payment.completed")
                .source("stripe")
                .payload("{\"test\": \"payload\"}")
                .headers("{\"content-type\": \"application/json\"}")
                .receivedAt(Instant.now())
                .processingStatus(status)
                .retryCount(0)
                .build();
    }

    public static WebhookAuditLog createWebhookAuditLog(String eventId, String eventType) {
        return WebhookAuditLog.builder()
                .id(1L)
                .eventId(eventId)
                .eventType(eventType)
                .source("stripe")
                .payload("{\"test\": \"payload\"}")
                .headers("{\"content-type\": \"application/json\"}")
                .receivedAt(Instant.now())
                .processingStatus(WebhookAuditLog.ProcessingStatus.PENDING)
                .retryCount(0)
                .build();
    }

    public static PaymentWebhookPayload createPaymentWebhookPayload() {
        return PaymentWebhookPayload.builder()
                .eventId("pay_evt_test_12345")
                .eventType("payment.completed")
                .source("stripe")
                .timestamp(Instant.now())
                .paymentData(PaymentWebhookPayload.PaymentData.builder()
                        .paymentId("pi_test_12345")
                        .orderId("order_test_12345")
                        .amount(new BigDecimal("99.99"))
                        .currency("USD")
                        .status("PAYMENT_SUCCEEDED")
                        .transactionId("txn_test_12345")
                        .paymentMethod("visa")
                        .build())
                .metadata(Map.of("customer_email", "test@example.com"))
                .build();
    }

    public static PaymentWebhookPayload createPaymentWebhookPayload(String status) {
        return PaymentWebhookPayload.builder()
                .eventId("pay_evt_test_12345")
                .eventType("payment.completed")
                .source("stripe")
                .timestamp(Instant.now())
                .paymentData(PaymentWebhookPayload.PaymentData.builder()
                        .paymentId("pi_test_12345")
                        .orderId("order_test_12345")
                        .amount(new BigDecimal("99.99"))
                        .currency("USD")
                        .status(status)
                        .transactionId("txn_test_12345")
                        .paymentMethod("visa")
                        .build())
                .metadata(Map.of("customer_email", "test@example.com"))
                .build();
    }

    public static ShipmentWebhookPayload createShipmentWebhookPayload() {
        return ShipmentWebhookPayload.builder()
                .eventId("ship_evt_test_12345")
                .eventType("shipment.shipped")
                .source("fedex")
                .timestamp(Instant.now())
                .shipmentData(ShipmentWebhookPayload.ShipmentData.builder()
                        .shipmentId("shp_test_12345")
                        .orderId("order_test_12345")
                        .trackingNumber("1234567890123456")
                        .carrier("fedex")
                        .status("SHIPPED")
                        .originAddress(ShipmentWebhookPayload.Address.builder()
                                .street("123 Warehouse St")
                                .city("New York")
                                .state("NY")
                                .postalCode("10001")
                                .country("US")
                                .build())
                        .destinationAddress(ShipmentWebhookPayload.Address.builder()
                                .street("456 Customer Ave")
                                .city("Los Angeles")
                                .state("CA")
                                .postalCode("90001")
                                .country("US")
                                .build())
                        .estimatedDelivery(Instant.now().plusSeconds(86400 * 5))
                        .packages(List.of(ShipmentWebhookPayload.Package.builder()
                                .packageId("pkg_test_12345")
                                .weight(2.5)
                                .weightUnit("kg")
                                .dimensions(ShipmentWebhookPayload.Dimensions.builder()
                                        .length(30.0)
                                        .width(20.0)
                                        .height(15.0)
                                        .unit("cm")
                                        .build())
                                .build()))
                        .build())
                .metadata(Map.of("warehouse_id", "wh_test_01"))
                .build();
    }

    public static ShipmentWebhookPayload createShipmentWebhookPayload(String status) {
        return ShipmentWebhookPayload.builder()
                .eventId("ship_evt_test_12345")
                .eventType("shipment.shipped")
                .source("fedex")
                .timestamp(Instant.now())
                .shipmentData(ShipmentWebhookPayload.ShipmentData.builder()
                        .shipmentId("shp_test_12345")
                        .orderId("order_test_12345")
                        .trackingNumber("1234567890123456")
                        .carrier("fedex")
                        .status(status)
                        .originAddress(ShipmentWebhookPayload.Address.builder()
                                .street("123 Warehouse St")
                                .city("New York")
                                .state("NY")
                                .postalCode("10001")
                                .country("US")
                                .build())
                        .destinationAddress(ShipmentWebhookPayload.Address.builder()
                                .street("456 Customer Ave")
                                .city("Los Angeles")
                                .state("CA")
                                .postalCode("90001")
                                .country("US")
                                .build())
                        .estimatedDelivery(Instant.now().plusSeconds(86400 * 5))
                        .packages(List.of(ShipmentWebhookPayload.Package.builder()
                                .packageId("pkg_test_12345")
                                .weight(2.5)
                                .weightUnit("kg")
                                .dimensions(ShipmentWebhookPayload.Dimensions.builder()
                                        .length(30.0)
                                        .width(20.0)
                                        .height(15.0)
                                        .unit("cm")
                                        .build())
                                .build()))
                        .build())
                .metadata(Map.of("warehouse_id", "wh_test_01"))
                .build();
    }

    public static EmailNotificationRequest createEmailNotificationRequest() {
        return EmailNotificationRequest.builder()
                .recipientEmail("test@example.com")
                .subject("Order Status Update")
                .body("Your order status has been updated to SHIPPED")
                .orderId("order_test_12345")
                .build();
    }

    public static EmailNotificationRequest createEmailNotificationRequest(String recipientEmail, String subject, String body) {
        return EmailNotificationRequest.builder()
                .recipientEmail(recipientEmail)
                .subject(subject)
                .body(body)
                .orderId("order_test_12345")
                .build();
    }

    public static OrderStub createOrderStub() {
        return new OrderStub("order_test_12345", "PENDING", new BigDecimal("99.99"));
    }

    public static UserStub createUserStub() {
        return new UserStub("user_test_12345", "test@example.com", "John Doe");
    }

    public static class OrderStub {
        private final String orderId;
        private final String status;
        private final BigDecimal totalAmount;

        public OrderStub(String orderId, String status, BigDecimal totalAmount) {
            this.orderId = orderId;
            this.status = status;
            this.totalAmount = totalAmount;
        }

        public String getOrderId() {
            return orderId;
        }

        public String getStatus() {
            return status;
        }

        public BigDecimal getTotalAmount() {
            return totalAmount;
        }
    }

    public static class UserStub {
        private final String userId;
        private final String email;
        private final String name;

        public UserStub(String userId, String email, String name) {
            this.userId = userId;
            this.email = email;
            this.name = name;
        }

        public String getUserId() {
            return userId;
        }

        public String getEmail() {
            return email;
        }

        public String getName() {
            return name;
        }
    }
}
