package com.adil.ordertracker.webhook.repository;

import com.adil.ordertracker.support.TestDataFactory;
import com.ordertracker.audit.WebhookAuditLog;
import com.ordertracker.audit.WebhookAuditLogRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class WebhookEventRepositoryTest {

    @Container
    @SuppressWarnings("resource")
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

    @Autowired
    private WebhookAuditLogRepository webhookAuditLogRepository;

    private WebhookAuditLog auditLog1;
    private WebhookAuditLog auditLog2;
    private WebhookAuditLog auditLog3;

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

        auditLog1 = TestDataFactory.createWebhookAuditLog("evt_001", "payment.completed");
        auditLog1.setProcessingStatus(WebhookAuditLog.ProcessingStatus.PROCESSED);
        auditLog1.setReceivedAt(Instant.now().minusSeconds(3600));

        auditLog2 = TestDataFactory.createWebhookAuditLog("evt_002", "payment.failed");
        auditLog2.setProcessingStatus(WebhookAuditLog.ProcessingStatus.FAILED);
        auditLog2.setReceivedAt(Instant.now().minusSeconds(1800));

        auditLog3 = TestDataFactory.createWebhookAuditLog("evt_003", "shipment.shipped");
        auditLog3.setProcessingStatus(WebhookAuditLog.ProcessingStatus.PENDING);
        auditLog3.setReceivedAt(Instant.now());

        webhookAuditLogRepository.save(auditLog1);
        webhookAuditLogRepository.save(auditLog2);
        webhookAuditLogRepository.save(auditLog3);
    }

    @Test
    void findByEventId_ShouldReturnAuditLog() {
        Optional<WebhookAuditLog> found = webhookAuditLogRepository.findByEventId("evt_001");

        assertTrue(found.isPresent());
        assertEquals("evt_001", found.get().getEventId());
        assertEquals("payment.completed", found.get().getEventType());
    }

    @Test
    void findByEventId_ShouldReturnEmptyForNonExistentEvent() {
        Optional<WebhookAuditLog> found = webhookAuditLogRepository.findByEventId("evt_999");

        assertFalse(found.isPresent());
    }

    @Test
    void existsByEventId_ShouldReturnTrueForExistingEvent() {
        boolean exists = webhookAuditLogRepository.existsByEventId("evt_001");

        assertTrue(exists);
    }

    @Test
    void existsByEventId_ShouldReturnFalseForNonExistentEvent() {
        boolean exists = webhookAuditLogRepository.existsByEventId("evt_999");

        assertFalse(exists);
    }

    @Test
    void findByProcessingStatus_ShouldReturnLogsByStatus() {
        List<WebhookAuditLog> pendingLogs = webhookAuditLogRepository.findByProcessingStatus(WebhookAuditLog.ProcessingStatus.PENDING);

        assertEquals(1, pendingLogs.size());
        assertEquals("evt_003", pendingLogs.get(0).getEventId());
    }

    @Test
    void findByEventTypeAndProcessingStatus_ShouldReturnMatchingLogs() {
        List<WebhookAuditLog> logs = webhookAuditLogRepository.findByEventTypeAndProcessingStatus(
                "payment.completed",
                WebhookAuditLog.ProcessingStatus.PROCESSED
        );

        assertEquals(1, logs.size());
        assertEquals("evt_001", logs.get(0).getEventId());
    }

    @Test
    void findFailedWebhooksForRetry_ShouldReturnFailedLogsWithRetryCountBelowLimit() {
        auditLog2.setRetryCount(2);
        webhookAuditLogRepository.save(auditLog2);

        List<WebhookAuditLog> failedLogs = webhookAuditLogRepository.findFailedWebhooksForRetry(
                WebhookAuditLog.ProcessingStatus.FAILED, 3
        );

        assertEquals(1, failedLogs.size());
        assertEquals("evt_002", failedLogs.get(0).getEventId());
    }

    @Test
    void findFailedWebhooksForRetry_ShouldNotReturnLogsAboveRetryLimit() {
        auditLog2.setRetryCount(5);
        webhookAuditLogRepository.save(auditLog2);

        List<WebhookAuditLog> failedLogs = webhookAuditLogRepository.findFailedWebhooksForRetry(
                WebhookAuditLog.ProcessingStatus.FAILED, 3
        );

        assertEquals(0, failedLogs.size());
    }

    @Test
    void findByReceivedAtBetween_ShouldReturnLogsInDateRange() {
        Instant startDate = Instant.now().minusSeconds(2000);
        Instant endDate = Instant.now().plusSeconds(100);

        List<WebhookAuditLog> logs = webhookAuditLogRepository.findByReceivedAtBetween(startDate, endDate);

        assertEquals(2, logs.size());
    }

    @Test
    void countByProcessingStatus_ShouldReturnCorrectCount() {
        long pendingCount = webhookAuditLogRepository.countByProcessingStatus(WebhookAuditLog.ProcessingStatus.PENDING);
        long processedCount = webhookAuditLogRepository.countByProcessingStatus(WebhookAuditLog.ProcessingStatus.PROCESSED);
        long failedCount = webhookAuditLogRepository.countByProcessingStatus(WebhookAuditLog.ProcessingStatus.FAILED);

        assertEquals(1, pendingCount);
        assertEquals(1, processedCount);
        assertEquals(1, failedCount);
    }

    @Test
    void deleteByReceivedAtBefore_ShouldDeleteOldLogs() {
        Instant cutoffDate = Instant.now().minusSeconds(3000);

        int deletedCount = webhookAuditLogRepository.deleteByReceivedAtBefore(cutoffDate);

        assertEquals(1, deletedCount);
        assertEquals(2, webhookAuditLogRepository.count());
    }

    @Test
    void save_ShouldPersistAuditLog() {
        WebhookAuditLog newLog = TestDataFactory.createWebhookAuditLog("evt_004", "payment.refunded");
        newLog.setProcessingStatus(WebhookAuditLog.ProcessingStatus.PENDING);

        WebhookAuditLog saved = webhookAuditLogRepository.save(newLog);

        assertNotNull(saved.getId());
        assertEquals("evt_004", saved.getEventId());
        assertEquals("payment.refunded", saved.getEventType());
    }

    @Test
    void update_ShouldModifyExistingAuditLog() {
        auditLog1.setProcessingStatus(WebhookAuditLog.ProcessingStatus.FAILED);
        auditLog1.setErrorMessage("Test error");

        WebhookAuditLog updated = webhookAuditLogRepository.save(auditLog1);

        assertEquals(WebhookAuditLog.ProcessingStatus.FAILED, updated.getProcessingStatus());
        assertEquals("Test error", updated.getErrorMessage());
    }
}
