package com.loyalty.automation.domain;

import com.loyalty.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "automations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Automation extends BaseEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 64)
    private TriggerType triggerType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "configuration", nullable = false, columnDefinition = "JSONB")
    private String configuration;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    public Automation(UUID id, UUID tenantId, String name, TriggerType triggerType, String configuration) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.tenantId = Objects.requireNonNull(tenantId, "Tenant ID cannot be null");
        this.name = Objects.requireNonNull(name, "Name cannot be null");
        this.triggerType = Objects.requireNonNull(triggerType, "Trigger type cannot be null");
        this.configuration = configuration != null ? configuration : "{}";
        this.enabled = true;
    }

    public void enable() {
        this.enabled = true;
    }

    public void disable() {
        this.enabled = false;
    }
}
