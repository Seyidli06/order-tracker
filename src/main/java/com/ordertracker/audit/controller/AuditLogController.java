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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Audit Log Management", description = "Admin APIs for viewing webhook event history and audit logs")
@SecurityRequirement(name = "bearerAuth")
public class AuditLogController {

    private final AuditService auditService;

    @GetMapping("/webhooks/{eventId}")
    @Operation(summary = "Get webhook log by event ID", description = "Retrieve a specific webhook audit log by its event ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved audit log"),
            @ApiResponse(responseCode = "404", description = "Audit log not found")
    })
    public ResponseEntity<WebhookAuditResponse> getWebhookByEventId(
            @Parameter(description = "Event ID from the webhook source") @PathVariable String eventId) {
        log.info("Fetching audit log for eventId: {}", eventId);
        WebhookAuditLog auditLog = auditService.findByEventId(eventId);
        return ResponseEntity.ok(WebhookAuditResponse.fromEntity(auditLog));
    }

    @GetMapping("/webhooks")
    @Operation(summary = "Get all webhooks by status", description = "Retrieve all webhook audit logs filtered by processing status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved audit logs")
    })
    public ResponseEntity<List<WebhookAuditResponse>> getWebhooksByStatus(
            @Parameter(description = "Processing status (PENDING, PROCESSED, FAILED)")
            @RequestParam WebhookAuditLog.ProcessingStatus status) {
        log.info("Fetching audit logs with status: {}", status);
        List<WebhookAuditLog> logs = auditService.findByStatus(status);
        List<WebhookAuditResponse> responses = logs.stream()
                .map(WebhookAuditResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/webhooks/range")
    @Operation(summary = "Get webhooks by date range", description = "Retrieve webhook audit logs within a specific date range")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved audit logs")
    })
    public ResponseEntity<List<WebhookAuditResponse>> getWebhooksByDateRange(
            @Parameter(description = "Start date (ISO format)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @Parameter(description = "End date (ISO format)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        log.info("Fetching audit logs between {} and {}", from, to);
        List<WebhookAuditLog> logs = auditService.findByDateRange(from, to);
        List<WebhookAuditResponse> responses = logs.stream()
                .map(WebhookAuditResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/webhooks/count")
    @Operation(summary = "Count webhooks by status", description = "Get the count of webhook audit logs by processing status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved count")
    })
    public ResponseEntity<Long> getWebhooksCountByStatus(
            @Parameter(description = "Processing status (PENDING, PROCESSED, FAILED)")
            @RequestParam WebhookAuditLog.ProcessingStatus status) {
        log.info("Counting audit logs with status: {}", status);
        long count = auditService.getCountByStatus(status);
        return ResponseEntity.ok(count);
    }
}
