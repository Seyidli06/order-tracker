package com.ordertracker.audit.controller;

import com.ordertracker.audit.AuditService;
import com.ordertracker.audit.WebhookAuditLog;
import com.ordertracker.audit.dto.WebhookAuditResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogControllerTest {

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AuditLogController auditLogController;

    private WebhookAuditLog auditLog;

    @BeforeEach
    void setUp() {
        auditLog = WebhookAuditLog.builder()
                .id(1L)
                .eventId("evt_test_12345")
                .eventType("PAYMENT_SUCCEEDED")
                .source("stripe")
                .payload("{\"test\":\"payload\"}")
                .headers("{\"authorization\":\"Bearer token\"}")
                .receivedAt(Instant.now())
                .processingStatus(WebhookAuditLog.ProcessingStatus.PROCESSED)
                .retryCount(0)
                .build();
    }

    @Test
    void getWebhookByEventId_ShouldReturnAuditLog() {
        when(auditService.findByEventId("evt_test_12345")).thenReturn(auditLog);

        ResponseEntity<WebhookAuditResponse> response = auditLogController.getWebhookByEventId("evt_test_12345");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("evt_test_12345", response.getBody().getEventId());
        assertEquals("PAYMENT_SUCCEEDED", response.getBody().getEventType());
        verify(auditService).findByEventId("evt_test_12345");
    }

    @Test
    void getWebhooksByStatus_ShouldReturnFilteredLogs() {
        when(auditService.findByStatus(WebhookAuditLog.ProcessingStatus.PROCESSED))
                .thenReturn(List.of(auditLog));

        ResponseEntity<List<WebhookAuditResponse>> response = auditLogController.getWebhooksByStatus(WebhookAuditLog.ProcessingStatus.PROCESSED);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("evt_test_12345", response.getBody().get(0).getEventId());
        verify(auditService).findByStatus(WebhookAuditLog.ProcessingStatus.PROCESSED);
    }

    @Test
    void getWebhooksByDateRange_ShouldReturnLogsInRange() {
        Instant from = Instant.now().minusSeconds(3600);
        Instant to = Instant.now();
        when(auditService.findByDateRange(from, to)).thenReturn(List.of(auditLog));

        ResponseEntity<List<WebhookAuditResponse>> response = auditLogController.getWebhooksByDateRange(from, to);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        verify(auditService).findByDateRange(from, to);
    }

    @Test
    void getWebhooksCountByStatus_ShouldReturnCount() {
        when(auditService.getCountByStatus(WebhookAuditLog.ProcessingStatus.FAILED)).thenReturn(5L);

        ResponseEntity<Long> response = auditLogController.getWebhooksCountByStatus(WebhookAuditLog.ProcessingStatus.FAILED);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(5L, response.getBody());
        verify(auditService).getCountByStatus(WebhookAuditLog.ProcessingStatus.FAILED);
    }
}
