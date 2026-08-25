package com.ordertracker.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface WebhookAuditLogRepository extends JpaRepository<WebhookAuditLog, Long> {

    Optional<WebhookAuditLog> findByEventId(String eventId);

    boolean existsByEventId(String eventId);

    List<WebhookAuditLog> findByProcessingStatus(WebhookAuditLog.ProcessingStatus status);

    List<WebhookAuditLog> findByEventTypeAndProcessingStatus(String eventType, WebhookAuditLog.ProcessingStatus status);

    @Query("SELECT w FROM WebhookAuditLog w WHERE w.processingStatus = :status AND w.retryCount < :maxRetries")
    List<WebhookAuditLog> findFailedWebhooksForRetry(@Param("status") WebhookAuditLog.ProcessingStatus status, @Param("maxRetries") int maxRetries);

    @Query("SELECT w FROM WebhookAuditLog w WHERE w.receivedAt BETWEEN :startDate AND :endDate")
    List<WebhookAuditLog> findByReceivedAtBetween(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    @Query("SELECT COUNT(w) FROM WebhookAuditLog w WHERE w.processingStatus = :status")
    long countByProcessingStatus(@Param("status") WebhookAuditLog.ProcessingStatus status);

    int deleteByReceivedAtBefore(Instant date);
}
