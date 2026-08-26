package com.adil.ordertracker.webhook.integration;

import com.adil.ordertracker.support.TestDataFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ordertracker.audit.WebhookAuditLog;
import com.ordertracker.audit.WebhookAuditLogRepository;
import com.ordertracker.webhook.dto.ShipmentWebhookPayload;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class ShipmentWebhookIntegrationTest {

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

    @BeforeAll
    static void beforeAll() {
        postgresContainer.start();
    }

    @BeforeEach
    void setUp() {
        webhookAuditLogRepository.deleteAll();
    }

    @Test
    void handleShipmentWebhook_ValidPayload_FullFlowWorks() throws Exception {
        ShipmentWebhookPayload payload = TestDataFactory.createShipmentWebhookPayload("SHIPPED");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(payload), headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/webhooks/shipment",
                request,
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());

        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    WebhookAuditLog savedLog = webhookAuditLogRepository.findByEventId(payload.getEventId())
                            .orElse(null);
                    assertNotNull(savedLog);
                    assertEquals(payload.getEventId(), savedLog.getEventId());
                    assertEquals(payload.getEventType(), savedLog.getEventType());
                });
    }

    @Test
    void handleShipmentWebhook_Delivered_FullFlowWorks() throws Exception {
        ShipmentWebhookPayload payload = TestDataFactory.createShipmentWebhookPayload("DELIVERED");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(payload), headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/webhooks/shipment",
                request,
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());

        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    WebhookAuditLog savedLog = webhookAuditLogRepository.findByEventId(payload.getEventId())
                            .orElse(null);
                    assertNotNull(savedLog);
                    assertEquals(payload.getEventId(), savedLog.getEventId());
                });
    }

    @Test
    void handleShipmentWebhook_OutForDelivery_FullFlowWorks() throws Exception {
        ShipmentWebhookPayload payload = TestDataFactory.createShipmentWebhookPayload("OUT_FOR_DELIVERY");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(payload), headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/webhooks/shipment",
                request,
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());

        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    WebhookAuditLog savedLog = webhookAuditLogRepository.findByEventId(payload.getEventId())
                            .orElse(null);
                    assertNotNull(savedLog);
                    assertEquals(payload.getEventId(), savedLog.getEventId());
                });
    }

    @Test
    void handleShipmentWebhook_DuplicateEvent_ShouldNotCreateDuplicate() throws Exception {
        ShipmentWebhookPayload payload = TestDataFactory.createShipmentWebhookPayload("SHIPPED");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(payload), headers);

        restTemplate.postForEntity(
                "http://localhost:" + port + "/api/webhooks/shipment",
                request,
                String.class
        );

        await().atMost(5, TimeUnit.SECONDS)
                .until(() -> webhookAuditLogRepository.findByEventId(payload.getEventId()).isPresent());

        long initialCount = webhookAuditLogRepository.count();

        restTemplate.postForEntity(
                "http://localhost:" + port + "/api/webhooks/shipment",
                request,
                String.class
        );

        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> assertEquals(initialCount, webhookAuditLogRepository.count()));
    }
}
