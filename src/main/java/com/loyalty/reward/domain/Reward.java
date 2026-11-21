package com.loyalty.reward.domain;

import com.loyalty.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "rewards")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reward extends BaseEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "points_cost", nullable = false)
    private long pointsCost;

    @Column(name = "active", nullable = false)
    private boolean active;

    public Reward(UUID id, UUID tenantId, String name, String description, long pointsCost) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.tenantId = Objects.requireNonNull(tenantId, "Tenant ID cannot be null");
        this.name = Objects.requireNonNull(name, "Reward name cannot be null");
        this.description = description != null ? description : "";
        if (pointsCost <= 0) {
            throw new IllegalArgumentException("Points cost must be strictly positive");
        }
        this.pointsCost = pointsCost;
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }
}
