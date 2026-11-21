package com.loyalty.automation.domain;

import com.loyalty.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "automation_executions", uniqueConstraints = {
    @UniqueConstraint(name = "uq_auto_exec_automation_idempotency", columnNames = {"automation_id", "idempotency_key"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AutomationExecution extends BaseEntity {

    @Id
    private UUID id;

    @Column(name = "automation_id", nullable = false)
    private UUID automationId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "trigger_event_id")
    private UUID triggerEventId;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ExecutionStatus status;

    @Column(name = "triggered_at", nullable = false)
    private Instant triggeredAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    public AutomationExecution(UUID id, UUID automationId, UUID customerId, UUID triggerEventId, String idempotencyKey) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.automationId = Objects.requireNonNull(automationId, "Automation ID cannot be null");
        this.customerId = Objects.requireNonNull(customerId, "Customer ID cannot be null");
        this.triggerEventId = triggerEventId;
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "Idempotency key cannot be null");
        this.status = ExecutionStatus.PENDING;
        this.triggeredAt = Instant.now();
    }

    public void markRunning() {
        this.status = ExecutionStatus.RUNNING;
    }

    public void markCompleted() {
        this.status = ExecutionStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    public void markFailed(String error) {
        this.status = ExecutionStatus.FAILED;
        this.errorMessage = error;
        this.completedAt = Instant.now();
    }
}
