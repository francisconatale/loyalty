package com.loyalty.coupon.domain;

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
@Table(name = "coupons", uniqueConstraints = {
    @UniqueConstraint(name = "uq_coupons_tenant_code", columnNames = {"tenant_id", "code"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon extends BaseEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "reward_id")
    private UUID rewardId;

    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 32)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 14, scale = 2)
    private BigDecimal discountValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private CouponStatus status;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "redeemed_at")
    private Instant redeemedAt;

    public Coupon(UUID id, UUID tenantId, UUID customerId, UUID rewardId, String code,
                  DiscountType discountType, BigDecimal discountValue, Instant expiresAt) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.tenantId = Objects.requireNonNull(tenantId, "Tenant ID cannot be null");
        this.customerId = Objects.requireNonNull(customerId, "Customer ID cannot be null");
        this.rewardId = rewardId;
        this.code = Objects.requireNonNull(code, "Code cannot be null").trim().toUpperCase();
        this.discountType = Objects.requireNonNull(discountType, "Discount type cannot be null");
        this.discountValue = Objects.requireNonNull(discountValue, "Discount value cannot be null");
        this.status = CouponStatus.AVAILABLE;
        this.expiresAt = expiresAt;
    }

    public boolean isUsable() {
        if (this.status != CouponStatus.AVAILABLE) {
            return false;
        }
        return this.expiresAt == null || this.expiresAt.isAfter(Instant.now());
    }

    public void redeem() {
        if (this.status == CouponStatus.REDEEMED) {
            throw new IllegalStateException("Coupon " + this.code + " has already been redeemed");
        }
        if (this.status == CouponStatus.CANCELLED) {
            throw new IllegalStateException("Coupon " + this.code + " has been cancelled");
        }
        if (this.expiresAt != null && this.expiresAt.isBefore(Instant.now())) {
            this.status = CouponStatus.EXPIRED;
            throw new IllegalStateException("Coupon " + this.code + " is expired");
        }
        this.status = CouponStatus.REDEEMED;
        this.redeemedAt = Instant.now();
    }
}
