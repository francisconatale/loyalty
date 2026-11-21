package com.loyalty.coupon.api.dto;

import com.loyalty.coupon.domain.Coupon;
import com.loyalty.coupon.domain.CouponStatus;
import com.loyalty.coupon.domain.DiscountType;

import java.math.BigDecimal;
import java.time.Instant;

public record CouponValidationResponseDTO(
    boolean isValid,
    String code,
    CouponStatus status,
    DiscountType discountType,
    BigDecimal discountValue,
    Instant expiresAt
) {
    public static CouponValidationResponseDTO from(Coupon coupon) {
        return new CouponValidationResponseDTO(
            coupon.isUsable(),
            coupon.getCode(),
            coupon.getStatus(),
            coupon.getDiscountType(),
            coupon.getDiscountValue(),
            coupon.getExpiresAt()
        );
    }
}
