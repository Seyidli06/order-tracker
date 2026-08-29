package com.ordertracker.audit.dto;

import com.ordertracker.audit.WebhookAuditLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookAuditResponse {

    private Long id;
    private String eventId;
    private String eventType;
    private String source;
    private String payload;
    private String headers;
    private Instant receivedAt;
    private Instant processedAt;
    private WebhookAuditLog.ProcessingStatus processingStatus;
    private String errorMessage;
    private Integer retryCount;
    private Instant createdAt;
    private Instant updatedAt;

    public static WebhookAuditResponse fromEntity(WebhookAuditLog entity) {
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
