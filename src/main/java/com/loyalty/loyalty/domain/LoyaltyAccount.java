package com.loyalty.loyalty.domain;

import com.loyalty.shared.domain.BaseEntity;
import com.loyalty.shared.exception.InsufficientPointsException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "loyalty_accounts", uniqueConstraints = {
    @UniqueConstraint(name = "uq_loyalty_accounts_program_customer", columnNames = {"program_id", "customer_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoyaltyAccount extends BaseEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "program_id", nullable = false)
    private UUID programId;

    @Column(name = "points_balance", nullable = false)
    private long pointsBalance;

    @Column(name = "lifetime_points", nullable = false)
    private long lifetimePoints;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public LoyaltyAccount(UUID id, UUID tenantId, UUID customerId, UUID programId) {
        this.id = Objects.requireNonNull(id, "Account ID cannot be null");
        this.tenantId = Objects.requireNonNull(tenantId, "Tenant ID cannot be null");
        this.customerId = Objects.requireNonNull(customerId, "Customer ID cannot be null");
        this.programId = Objects.requireNonNull(programId, "Program ID cannot be null");
        this.pointsBalance = 0;
        this.lifetimePoints = 0;
    }

    public void accruePoints(long points) {
        if (points <= 0) {
            throw new IllegalArgumentException("Accrual points must be strictly positive");
        }
        this.pointsBalance += points;
        this.lifetimePoints += points;
    }

    public void redeemPoints(long points) {
        if (points <= 0) {
            throw new IllegalArgumentException("Redemption points must be strictly positive");
        }
        if (points > this.pointsBalance) {
            throw new InsufficientPointsException("Insufficient point balance: current=" + this.pointsBalance + ", requested=" + points);
        }
        this.pointsBalance -= points;
    }

    public void reversePoints(long points) {
        if (points <= 0) {
            throw new IllegalArgumentException("Reversal points must be strictly positive");
        }
        this.pointsBalance = Math.max(0, this.pointsBalance - points);
    }
}
