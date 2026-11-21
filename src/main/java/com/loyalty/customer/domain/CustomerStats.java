package com.loyalty.customer.domain;

import com.loyalty.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "customer_stats")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomerStats extends BaseEntity {

    @Id
    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "total_purchases", nullable = false)
    private int totalPurchases;

    @Column(name = "total_spent", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalSpent;

    @Column(name = "average_ticket", nullable = false, precision = 14, scale = 2)
    private BigDecimal averageTicket;

    @Column(name = "first_purchase_at")
    private Instant firstPurchaseAt;

    @Column(name = "last_purchase_at")
    private Instant lastPurchaseAt;

    public CustomerStats(UUID customerId, UUID tenantId) {
        this.customerId = Objects.requireNonNull(customerId, "Customer ID cannot be null");
        this.tenantId = Objects.requireNonNull(tenantId, "Tenant ID cannot be null");
        this.totalPurchases = 0;
        this.totalSpent = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.averageTicket = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public void recordPurchase(BigDecimal amount, Instant purchasedAt) {
        this.totalPurchases += 1;
        this.totalSpent = this.totalSpent.add(amount).setScale(2, RoundingMode.HALF_UP);
        this.averageTicket = this.totalSpent.divide(BigDecimal.valueOf(this.totalPurchases), 2, RoundingMode.HALF_UP);
        if (this.firstPurchaseAt == null || (purchasedAt != null && purchasedAt.isBefore(this.firstPurchaseAt))) {
            this.firstPurchaseAt = purchasedAt;
        }
        if (this.lastPurchaseAt == null || (purchasedAt != null && purchasedAt.isAfter(this.lastPurchaseAt))) {
            this.lastPurchaseAt = purchasedAt;
        }
    }

    public void recordRefund(BigDecimal amount) {
        this.totalSpent = this.totalSpent.subtract(amount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        if (this.totalPurchases > 0) {
            this.averageTicket = this.totalSpent.divide(BigDecimal.valueOf(this.totalPurchases), 2, RoundingMode.HALF_UP);
        }
    }
}
