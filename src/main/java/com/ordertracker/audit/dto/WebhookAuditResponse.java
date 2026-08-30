package com.ordertracker.audit.dto;

import com.ordertracker.audit.WebhookAuditLog;
import lombok.Builder;

import java.time.Instant;

@Builder
public record WebhookAuditResponse(
        Long id,
        String eventId,
        String eventType,
        String source,
        String payload,
        String headers,
        Instant receivedAt,
        Instant processedAt,
        WebhookAuditLog.ProcessingStatus processingStatus,
        String errorMessage,
        Integer retryCount,
        Instant createdAt,
        Instant updatedAt
) {

    public static WebhookAuditResponse fromEntity(
            WebhookAuditLog entity
    ) {
        return WebhookAuditResponse.builder()
                .id(entity.getId())
                .eventId(entity.getEventId())
                .eventType(entity.getEventType())
                .source(entity.getSource())
                .payload(entity.getPayload())
                .headers(entity.getHeaders())
                .receivedAt(entity.getReceivedAt())
                .processedAt(entity.getProcessedAt())
                .processingStatus(entity.getProcessingStatus())
                .errorMessage(entity.getErrorMessage())
                .retryCount(entity.getRetryCount())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}