package com.adil.ordertracker.integration;

import com.adil.ordertracker.support.TestDataFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ordertracker.audit.WebhookAuditLog;
import com.ordertracker.audit.WebhookAuditLogRepository;
import com.ordertracker.webhook.dto.PaymentWebhookPayload;
import com.ordertracker.webhook.dto.ShipmentWebhookPayload;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class OrderWebhookFlowIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", postgresContainer::getDriverClassName);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WebhookAuditLogRepository webhookAuditLogRepository;

    @MockitoBean
    private JavaMailSender javaMailSender;

    private static final String ORDER_ID = "order_e2e_test_12345";

    @BeforeAll
    static void beforeAll() {
        postgresContainer.start();
    }

    @AfterAll
    static void afterAll() {
        postgresContainer.stop();
    }

    @BeforeEach
    void setUp() {
        webhookAuditLogRepository.deleteAll();
    }

    @Test
    void endToEndWebhookFlow_PaymentSuccessToShipped_CompleteFlow() throws Exception {
        String paymentEventId = "pay_e2e_evt_001";
        String shipmentEventId = "ship_e2e_evt_001";

        PaymentWebhookPayload paymentPayload = TestDataFactory.createPaymentWebhookPayload("PAYMENT_SUCCEEDED");
        paymentPayload.setEventId(paymentEventId);
        paymentPayload.getPaymentData().setOrderId(ORDER_ID);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> paymentRequest = new HttpEntity<>(objectMapper.writeValueAsString(paymentPayload), headers);

        ResponseEntity<String> paymentResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/webhooks/payment",
                paymentRequest,
                String.class
        );

        assertEquals(HttpStatus.OK, paymentResponse.getStatusCode());

        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    WebhookAuditLog paymentLog = webhookAuditLogRepository.findByEventId(paymentEventId)
                            .orElse(null);
                    assertNotNull(paymentLog, "Payment webhook should be logged in audit table");
                    assertEquals(paymentEventId, paymentLog.getEventId());
                    assertEquals("payment.completed", paymentLog.getEventType());
                    assertEquals(WebhookAuditLog.ProcessingStatus.PROCESSED, paymentLog.getProcessingStatus());
                });

        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verify(javaMailSender, times(1)).send(any(SimpleMailMessage.class));
                });

        ShipmentWebhookPayload shipmentPayload = TestDataFactory.createShipmentWebhookPayload("SHIPPED");
        shipmentPayload.setEventId(shipmentEventId);
        shipmentPayload.getShipmentData().setOrderId(ORDER_ID);

        HttpEntity<String> shipmentRequest = new HttpEntity<>(objectMapper.writeValueAsString(shipmentPayload), headers);

        ResponseEntity<String> shipmentResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/webhooks/shipment",
                shipmentRequest,
                String.class
        );

        assertEquals(HttpStatus.OK, shipmentResponse.getStatusCode());

        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    WebhookAuditLog shipmentLog = webhookAuditLogRepository.findByEventId(shipmentEventId)
                            .orElse(null);
                    assertNotNull(shipmentLog, "Shipment webhook should be logged in audit table");
                    assertEquals(shipmentEventId, shipmentLog.getEventId());
                    assertEquals("shipment.shipped", shipmentLog.getEventType());
                    assertEquals(WebhookAuditLog.ProcessingStatus.PROCESSED, shipmentLog.getProcessingStatus());
                });

        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verify(javaMailSender, times(2)).send(any(SimpleMailMessage.class));
                });

        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    long totalLogs = webhookAuditLogRepository.count();
                    assertEquals(2, totalLogs, "Should have exactly 2 webhook audit logs");
                });
    }

    @Test
    void endToEndWebhookFlow_PaymentFailedToShipped_CompleteFlow() throws Exception {
        String paymentEventId = "pay_e2e_evt_002";
        String shipmentEventId = "ship_e2e_evt_002";

        PaymentWebhookPayload paymentPayload = TestDataFactory.createPaymentWebhookPayload("PAYMENT_FAILED");
        paymentPayload.setEventId(paymentEventId);
        paymentPayload.getPaymentData().setOrderId(ORDER_ID);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> paymentRequest = new HttpEntity<>(objectMapper.writeValueAsString(paymentPayload), headers);

        ResponseEntity<String> paymentResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/webhooks/payment",
                paymentRequest,
                String.class
        );

        assertEquals(HttpStatus.OK, paymentResponse.getStatusCode());

        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    WebhookAuditLog paymentLog = webhookAuditLogRepository.findByEventId(paymentEventId)
                            .orElse(null);
                    assertNotNull(paymentLog);
                    assertEquals(WebhookAuditLog.ProcessingStatus.PROCESSED, paymentLog.getProcessingStatus());
                });

        ShipmentWebhookPayload shipmentPayload = TestDataFactory.createShipmentWebhookPayload("DELIVERED");
        shipmentPayload.setEventId(shipmentEventId);
        shipmentPayload.getShipmentData().setOrderId(ORDER_ID);

        HttpEntity<String> shipmentRequest = new HttpEntity<>(objectMapper.writeValueAsString(shipmentPayload), headers);

        ResponseEntity<String> shipmentResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/webhooks/shipment",
                shipmentRequest,
                String.class
        );

        assertEquals(HttpStatus.OK, shipmentResponse.getStatusCode());

        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    WebhookAuditLog shipmentLog = webhookAuditLogRepository.findByEventId(shipmentEventId)
                            .orElse(null);
                    assertNotNull(shipmentLog);
                    assertEquals(WebhookAuditLog.ProcessingStatus.PROCESSED, shipmentLog.getProcessingStatus());
                });

        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verify(javaMailSender, times(2)).send(any(SimpleMailMessage.class));
                });
    }

    @Test
    void endToEndWebhookFlow_DuplicatePaymentEvent_PreventsDuplicateProcessing() throws Exception {
        String paymentEventId = "pay_e2e_evt_003";

        PaymentWebhookPayload paymentPayload = TestDataFactory.createPaymentWebhookPayload("PAYMENT_SUCCEEDED");
        paymentPayload.setEventId(paymentEventId);
        paymentPayload.getPaymentData().setOrderId(ORDER_ID);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(paymentPayload), headers);

        restTemplate.postForEntity(
                "http://localhost:" + port + "/api/webhooks/payment",
                request,
                String.class
        );

        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> webhookAuditLogRepository.findByEventId(paymentEventId).isPresent());

        long initialCount = webhookAuditLogRepository.count();
        long initialEmailCalls = 1;

        restTemplate.postForEntity(
                "http://localhost:" + port + "/api/webhooks/payment",
                request,
                String.class
        );

        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> assertEquals(initialCount, webhookAuditLogRepository.count()));

        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verify(javaMailSender, times((int) initialEmailCalls)).send(any(SimpleMailMessage.class));
                });
    }

    @Test
    void endToEndWebhookFlow_MultipleShipmentsForSameOrder_ProcessesAll() throws Exception {
        String shipmentEventId1 = "ship_e2e_evt_004";
        String shipmentEventId2 = "ship_e2e_evt_005";

        ShipmentWebhookPayload shipmentPayload1 = TestDataFactory.createShipmentWebhookPayload("SHIPPED");
        shipmentPayload1.setEventId(shipmentEventId1);
        shipmentPayload1.getShipmentData().setOrderId(ORDER_ID);

        ShipmentWebhookPayload shipmentPayload2 = TestDataFactory.createShipmentWebhookPayload("OUT_FOR_DELIVERY");
        shipmentPayload2.setEventId(shipmentEventId2);
        shipmentPayload2.getShipmentData().setOrderId(ORDER_ID);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> request1 = new HttpEntity<>(objectMapper.writeValueAsString(shipmentPayload1), headers);
        HttpEntity<String> request2 = new HttpEntity<>(objectMapper.writeValueAsString(shipmentPayload2), headers);

        restTemplate.postForEntity(
                "http://localhost:" + port + "/api/webhooks/shipment",
                request1,
                String.class
        );

        restTemplate.postForEntity(
                "http://localhost:" + port + "/api/webhooks/shipment",
                request2,
                String.class
        );

        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    WebhookAuditLog log1 = webhookAuditLogRepository.findByEventId(shipmentEventId1).orElse(null);
                    WebhookAuditLog log2 = webhookAuditLogRepository.findByEventId(shipmentEventId2).orElse(null);
                    assertNotNull(log1);
                    assertNotNull(log2);
                });

        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verify(javaMailSender, times(2)).send(any(SimpleMailMessage.class));
                });
    }
}
