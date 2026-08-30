package com.ordertracker.audit.controller;

import com.ordertracker.audit.AuditService;
import com.ordertracker.audit.WebhookAuditLog;
import com.ordertracker.audit.dto.WebhookAuditResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@Slf4j
@Tag(
        name = "Audit Log Management",
        description = "Admin APIs for viewing webhook event history and audit logs"
)
@SecurityRequirement(name = "bearerAuth")
public class AuditLogController {

    private final AuditService auditService;

    @GetMapping("/logs/{eventId}")
    @Operation(
            summary = "Get webhook log by event ID",
            description = "Retrieve a specific webhook audit log by its event ID"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved audit log"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "ADMIN role is required"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Audit log not found"
            )
    })
    public ResponseEntity<WebhookAuditResponse> getLogByEventId(
            @Parameter(
                    description = "Event ID from the webhook source"
            )
            @PathVariable
            String eventId
    ) {
        log.info(
                "Fetching audit log for eventId={}",
                eventId
        );

        WebhookAuditLog auditLog =
                auditService.findByEventId(
                        eventId
                );

        return ResponseEntity.ok(
                WebhookAuditResponse.fromEntity(
                        auditLog
                )
        );
    }

    @GetMapping("/logs")
    @Operation(
            summary = "Get logs by status",
            description = "Retrieve webhook audit logs filtered by processing status"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved audit logs"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "ADMIN role is required"
            )
    })
    public ResponseEntity<List<WebhookAuditResponse>>
    getLogsByStatus(
            @Parameter(
                    description = "Processing status: PENDING, PROCESSED or FAILED"
            )
            @RequestParam
            WebhookAuditLog.ProcessingStatus status
    ) {
        log.info(
                "Fetching audit logs with status={}",
                status
        );

        List<WebhookAuditResponse> response =
                auditService
                        .findByStatus(status)
                        .stream()
                        .map(
                                WebhookAuditResponse::fromEntity
                        )
                        .toList();

        return ResponseEntity.ok(
                response
        );
    }

    @GetMapping("/logs/date-range")
    @Operation(
            summary = "Get logs by date range",
            description = "Retrieve webhook audit logs within a specific date range"
    )
    public ResponseEntity<List<WebhookAuditResponse>>
    getLogsByDateRange(
            @Parameter(
                    description = "Start date in ISO-8601 format"
            )
            @RequestParam
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME
            )
            Instant startDate,

            @Parameter(
                    description = "End date in ISO-8601 format"
            )
            @RequestParam
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME
            )
            Instant endDate
    ) {
        log.info(
                "Fetching audit logs between {} and {}",
                startDate,
                endDate
        );

        List<WebhookAuditResponse> response =
                auditService
                        .findByDateRange(
                                startDate,
                                endDate
                        )
                        .stream()
                        .map(
                                WebhookAuditResponse::fromEntity
                        )
                        .toList();

        return ResponseEntity.ok(
                response
        );
    }

    @GetMapping("/logs/failed/retry")
    @Operation(
            summary = "Get failed logs for retry",
            description = "Retrieve failed webhook logs eligible for retry"
    )
    public ResponseEntity<List<WebhookAuditResponse>>
    getFailedLogsForRetry(
            @Parameter(
                    description = "Maximum retry count"
            )
            @RequestParam(
                    defaultValue = "3"
            )
            int maxRetries
    ) {
        log.info(
                "Fetching failed audit logs with maxRetries={}",
                maxRetries
        );

        List<WebhookAuditResponse> response =
                auditService
                        .findFailedWebhooksForRetry(
                                maxRetries
                        )
                        .stream()
                        .map(
                                WebhookAuditResponse::fromEntity
                        )
                        .toList();

        return ResponseEntity.ok(
                response
        );
    }

    @GetMapping("/logs/count")
    @Operation(
            summary = "Count logs by status",
            description = "Get the number of webhook audit logs by processing status"
    )
    public ResponseEntity<Long> getCountByStatus(
            @Parameter(
                    description = "Processing status: PENDING, PROCESSED or FAILED"
            )
            @RequestParam
            WebhookAuditLog.ProcessingStatus status
    ) {
        return ResponseEntity.ok(
                auditService.getCountByStatus(
                        status
                )
        );
    }
}