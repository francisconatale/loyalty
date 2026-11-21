package com.loyalty.purchase.domain;

import com.loyalty.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "purchases", uniqueConstraints = {
    @UniqueConstraint(name = "uq_purchases_integration_external", columnNames = {"integration_id", "external_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Purchase extends BaseEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "integration_id", nullable = false)
    private UUID integrationId;

    @Column(name = "external_id", nullable = false, length = 128)
    private String externalId;

    @Column(name = "total", nullable = false, precision = 14, scale = 2)
    private BigDecimal total;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private PurchaseStatus status;

    @Column(name = "purchased_at", nullable = false)
    private Instant purchasedAt;

    public Purchase(UUID id, UUID tenantId, UUID customerId, UUID integrationId,
                    String externalId, BigDecimal total, String currency, Instant purchasedAt) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.tenantId = Objects.requireNonNull(tenantId, "Tenant ID cannot be null");
        this.customerId = Objects.requireNonNull(customerId, "Customer ID cannot be null");
        this.integrationId = Objects.requireNonNull(integrationId, "Integration ID cannot be null");
        this.externalId = Objects.requireNonNull(externalId, "External ID cannot be null");
        this.total = Objects.requireNonNull(total, "Total cannot be null");
        this.currency = currency != null ? currency.toUpperCase() : "ARS";
        this.status = PurchaseStatus.COMPLETED;
        this.purchasedAt = purchasedAt != null ? purchasedAt : Instant.now();
    }

    public void cancel() {
        if (this.status == PurchaseStatus.CANCELLED) {
            return;
        }
        this.status = PurchaseStatus.CANCELLED;
    }

    public void refund() {
        if (this.status == PurchaseStatus.REFUNDED) {
            return;
        }
        this.status = PurchaseStatus.REFUNDED;
    }
}
