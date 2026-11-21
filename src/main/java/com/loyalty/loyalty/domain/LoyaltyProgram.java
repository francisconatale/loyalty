package com.loyalty.loyalty.domain;

import com.loyalty.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "loyalty_programs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoyaltyProgram extends BaseEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "type", nullable = false, length = 32)
    private String type;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "configuration", nullable = false, columnDefinition = "JSONB")
    private String configuration;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    public LoyaltyProgram(UUID id, UUID tenantId, String name, String type, String configuration) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.tenantId = Objects.requireNonNull(tenantId, "Tenant ID cannot be null");
        this.name = Objects.requireNonNull(name, "Program name cannot be null");
        this.type = type != null ? type : "POINTS";
        this.configuration = configuration != null ? configuration : "{\"pointsPerSpent\": 100}";
        this.enabled = true;
    }

    public void disable() {
        this.enabled = false;
    }

    public void enable() {
        this.enabled = true;
    }
}
