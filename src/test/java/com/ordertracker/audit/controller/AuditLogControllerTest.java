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
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

        Instant now = Instant.now();

        auditLog =
                WebhookAuditLog
                        .builder()
                        .id(1L)
                        .eventId("evt_test_12345")
                        .eventType("PAYMENT_SUCCEEDED")
                        .source("stripe")
                        .payload("{\"orderId\":\"order_123\"}")
                        .headers("{\"x-signature\":\"test\"}")
                        .receivedAt(
                                now.minusSeconds(60)
                        )
                        .processedAt(now)
                        .processingStatus(
                                WebhookAuditLog
                                        .ProcessingStatus
                                        .PROCESSED
                        )
                        .errorMessage(null)
                        .retryCount(0)
                        .createdAt(
                                now.minusSeconds(60)
                        )
                        .updatedAt(now)
                        .build();
    }

    @Test
    void getLogByEventId_ShouldReturnMappedResponse() {

        when(
                auditService.findByEventId(
                        "evt_test_12345"
                )
        ).thenReturn(
                auditLog
        );

        ResponseEntity<WebhookAuditResponse> response =
                auditLogController
                        .getLogByEventId(
                                "evt_test_12345"
                        );

        assertEquals(
                200,
                response.getStatusCode().value()
        );

        assertNotNull(
                response.getBody()
        );

        assertEquals(
                auditLog.getId(),
                response.getBody().id()
        );

        assertEquals(
                auditLog.getEventId(),
                response.getBody().eventId()
        );

        assertEquals(
                auditLog.getEventType(),
                response.getBody().eventType()
        );

        assertEquals(
                auditLog.getSource(),
                response.getBody().source()
        );

        assertEquals(
                auditLog.getProcessingStatus(),
                response.getBody().processingStatus()
        );

        verify(
                auditService
        ).findByEventId(
                "evt_test_12345"
        );
    }

    @Test
    void getLogsByStatus_ShouldReturnMappedResponses() {

        when(
                auditService.findByStatus(
                        WebhookAuditLog
                                .ProcessingStatus
                                .PROCESSED
                )
        ).thenReturn(
                List.of(
                        auditLog
                )
        );

        ResponseEntity<List<WebhookAuditResponse>>
                response =
                auditLogController
                        .getLogsByStatus(
                                WebhookAuditLog
                                        .ProcessingStatus
                                        .PROCESSED
                        );

        assertEquals(
                200,
                response.getStatusCode().value()
        );

        assertNotNull(
                response.getBody()
        );

        assertEquals(
                1,
                response.getBody().size()
        );

        assertEquals(
                "evt_test_12345",
                response.getBody()
                        .getFirst()
                        .eventId()
        );

        verify(
                auditService
        ).findByStatus(
                WebhookAuditLog
                        .ProcessingStatus
                        .PROCESSED
        );
    }

    @Test
    void getLogsByDateRange_ShouldReturnMappedResponses() {

        Instant startDate =
                Instant.now()
                        .minusSeconds(3600);

        Instant endDate =
                Instant.now();

        when(
                auditService.findByDateRange(
                        startDate,
                        endDate
                )
        ).thenReturn(
                List.of(
                        auditLog
                )
        );

        ResponseEntity<List<WebhookAuditResponse>>
                response =
                auditLogController
                        .getLogsByDateRange(
                                startDate,
                                endDate
                        );

        assertEquals(
                200,
                response.getStatusCode().value()
        );

        assertNotNull(
                response.getBody()
        );

        assertEquals(
                1,
                response.getBody().size()
        );

        assertEquals(
                "evt_test_12345",
                response.getBody()
                        .getFirst()
                        .eventId()
        );

        verify(
                auditService
        ).findByDateRange(
                startDate,
                endDate
        );
    }

    @Test
    void getFailedLogsForRetry_ShouldReturnMappedResponses() {

        WebhookAuditLog failedLog =
                WebhookAuditLog
                        .builder()
                        .id(2L)
                        .eventId(
                                "evt_failed_12345"
                        )
                        .eventType(
                                "PAYMENT_FAILED"
                        )
                        .source("stripe")
                        .payload("{}")
                        .headers("{}")
                        .receivedAt(
                                Instant.now()
                        )
                        .processingStatus(
                                WebhookAuditLog
                                        .ProcessingStatus
                                        .FAILED
                        )
                        .errorMessage(
                                "Provider timeout"
                        )
                        .retryCount(1)
                        .createdAt(
                                Instant.now()
                        )
                        .updatedAt(
                                Instant.now()
                        )
                        .build();

        when(
                auditService
                        .findFailedWebhooksForRetry(
                                3
                        )
        ).thenReturn(
                List.of(
                        failedLog
                )
        );

        ResponseEntity<List<WebhookAuditResponse>>
                response =
                auditLogController
                        .getFailedLogsForRetry(
                                3
                        );

        assertEquals(
                200,
                response.getStatusCode().value()
        );

        assertNotNull(
                response.getBody()
        );

        assertEquals(
                1,
                response.getBody().size()
        );

        assertEquals(
                WebhookAuditLog
                        .ProcessingStatus
                        .FAILED,
                response.getBody()
                        .getFirst()
                        .processingStatus()
        );

        assertEquals(
                1,
                response.getBody()
                        .getFirst()
                        .retryCount()
        );

        assertEquals(
                "Provider timeout",
                response.getBody()
                        .getFirst()
                        .errorMessage()
        );

        verify(
                auditService
        ).findFailedWebhooksForRetry(
                3
        );
    }

    @Test
    void getCountByStatus_ShouldReturnCount() {

        when(
                auditService.getCountByStatus(
                        WebhookAuditLog
                                .ProcessingStatus
                                .FAILED
                )
        ).thenReturn(
                5L
        );

        ResponseEntity<Long> response =
                auditLogController
                        .getCountByStatus(
                                WebhookAuditLog
                                        .ProcessingStatus
                                        .FAILED
                        );

        assertEquals(
                200,
                response.getStatusCode().value()
        );

        assertEquals(
                5L,
                response.getBody()
        );

        verify(
                auditService
        ).getCountByStatus(
                WebhookAuditLog
                        .ProcessingStatus
                        .FAILED
        );
    }
}