package com.loyalty.integration.domain;

import com.loyalty.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "integration_events", uniqueConstraints = {
    @UniqueConstraint(name = "uq_integration_events_int_external", columnNames = {"integration_id", "external_event_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IntegrationEvent extends BaseEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "integration_id", nullable = false)
    private UUID integrationId;

    @Column(name = "external_event_id", nullable = false, length = 128)
    private String externalEventId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "JSONB")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private EventStatus status;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    public IntegrationEvent(UUID id, UUID tenantId, UUID integrationId, String externalEventId,
                            String eventType, String payload) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.tenantId = Objects.requireNonNull(tenantId, "Tenant ID cannot be null");
        this.integrationId = Objects.requireNonNull(integrationId, "Integration ID cannot be null");
        this.externalEventId = Objects.requireNonNull(externalEventId, "External event ID cannot be null");
        this.eventType = Objects.requireNonNull(eventType, "Event type cannot be null");
        this.payload = Objects.requireNonNull(payload, "Payload cannot be null");
        this.status = EventStatus.PENDING;
        this.receivedAt = Instant.now();
    }

    public void markProcessing() {
        this.status = EventStatus.PROCESSING;
    }

    public void markProcessed() {
        this.status = EventStatus.PROCESSED;
        this.processedAt = Instant.now();
    }

    public void markFailed() {
        this.status = EventStatus.FAILED;
    }
}
