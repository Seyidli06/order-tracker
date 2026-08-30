package com.ordertracker.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import jakarta.persistence.EntityNotFoundException;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final WebhookAuditLogRepository webhookAuditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public WebhookAuditLog logIncomingWebhook(String eventId, String eventType, String source, String payload, String headers) {
        if (webhookAuditLogRepository.existsByEventId(eventId)) {
            log.warn("Duplicate webhook event received: {}", eventId);
            throw new IllegalArgumentException("Webhook event with ID " + eventId + " already exists");
        }

        WebhookAuditLog auditLog = WebhookAuditLog.builder()
                .eventId(eventId)
                .eventType(eventType)
                .source(source)
                .payload(payload)
                .headers(headers)
                .receivedAt(Instant.now())
                .processingStatus(WebhookAuditLog.ProcessingStatus.PENDING)
                .retryCount(0)
                .build();

        WebhookAuditLog saved = webhookAuditLogRepository.save(auditLog);
        log.info("Logged incoming webhook: eventId={}, eventType={}, source={}", eventId, eventType, source);
        return saved;
    }

    @Async
    @Transactional
    public void markAsProcessed(Long auditLogId) {
        webhookAuditLogRepository.findById(auditLogId).ifPresentOrElse(auditLog -> {
            auditLog.setProcessingStatus(WebhookAuditLog.ProcessingStatus.PROCESSED);
            auditLog.setProcessedAt(Instant.now());
            webhookAuditLogRepository.save(auditLog);
            log.info("Marked webhook as processed: auditLogId={}, eventId={}", auditLogId, auditLog.getEventId());
        }, () -> log.error("Audit log not found for ID: {}", auditLogId));
    }

    @Async
    @Transactional
    public void markAsFailed(Long auditLogId, String errorMessage) {
        webhookAuditLogRepository.findById(auditLogId).ifPresentOrElse(auditLog -> {
            auditLog.setProcessingStatus(WebhookAuditLog.ProcessingStatus.FAILED);
            auditLog.setErrorMessage(errorMessage);
            auditLog.setRetryCount(auditLog.getRetryCount() + 1);
            webhookAuditLogRepository.save(auditLog);
            log.error("Marked webhook as failed: auditLogId={}, eventId={}, error={}", auditLogId, auditLog.getEventId(), errorMessage);
        }, () -> log.error("Audit log not found for ID: {}", auditLogId));
    }

    @Transactional(readOnly = true)
    public WebhookAuditLog findByEventId(
            String eventId
    ) {
        return webhookAuditLogRepository
                .findByEventId(eventId)
                .orElseThrow(
                        () -> new EntityNotFoundException(
                                "Webhook audit log not found for event ID: "
                                        + eventId
                        )
                );
    }

    @Transactional(readOnly = true)
    public List<WebhookAuditLog> findByStatus(WebhookAuditLog.ProcessingStatus status) {
        return webhookAuditLogRepository.findByProcessingStatus(status);
    }

    @Transactional(readOnly = true)
    public List<WebhookAuditLog> findFailedWebhooksForRetry(int maxRetries) {
        return webhookAuditLogRepository.findFailedWebhooksForRetry(WebhookAuditLog.ProcessingStatus.FAILED, maxRetries);
    }

    @Transactional(readOnly = true)
    public List<WebhookAuditLog> findByDateRange(Instant startDate, Instant endDate) {
        return webhookAuditLogRepository.findByReceivedAtBetween(startDate, endDate);
    }

    @Transactional(readOnly = true)
    public long getCountByStatus(WebhookAuditLog.ProcessingStatus status) {
        return webhookAuditLogRepository.countByProcessingStatus(status);
    }

    @Transactional
    public void cleanupOldLogs(Instant beforeDate) {
        int deletedCount = webhookAuditLogRepository.deleteByReceivedAtBefore(beforeDate);
        log.info("Cleaned up {} old webhook audit logs received before {}", deletedCount, beforeDate);
    }
}
