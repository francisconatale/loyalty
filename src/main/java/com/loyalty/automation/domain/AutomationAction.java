package com.loyalty.automation.domain;

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
@Table(name = "automation_actions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AutomationAction extends BaseEntity {

    @Id
    private UUID id;

    @Column(name = "execution_id", nullable = false)
    private UUID executionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 64)
    private ActionType actionType;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result", nullable = false, columnDefinition = "JSONB")
    private String result;

    @Column(name = "executed_at", nullable = false)
    private Instant executedAt;

    public AutomationAction(UUID id, UUID executionId, ActionType actionType, String status, String result) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.executionId = Objects.requireNonNull(executionId, "Execution ID cannot be null");
        this.actionType = Objects.requireNonNull(actionType, "Action type cannot be null");
        this.status = status != null ? status : "SUCCESS";
        this.result = result != null ? result : "{}";
        this.executedAt = Instant.now();
    }
}
