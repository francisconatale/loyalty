package com.loyalty.tenant.domain;

import com.loyalty.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "tenants")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tenant extends BaseEntity {

    @Id
    private UUID id;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private TenantStatus status;

    public Tenant(UUID id, String name) {
        this.id = Objects.requireNonNull(id, "Tenant ID cannot be null");
        this.name = Objects.requireNonNull(name, "Tenant name cannot be null");
        this.status = TenantStatus.ACTIVE;
    }

    public void suspend() {
        this.status = TenantStatus.SUSPENDED;
    }

    public void activate() {
        this.status = TenantStatus.ACTIVE;
    }

    public boolean isActive() {
        return this.status == TenantStatus.ACTIVE;
    }
}
