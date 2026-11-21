package com.loyalty.coupon.api.dto;

import com.loyalty.coupon.domain.Coupon;
import com.loyalty.coupon.domain.CouponStatus;

import java.time.Instant;

public record RedeemCouponResponseDTO(
    String code,
    CouponStatus status,
    Instant redeemedAt
) {
    public static RedeemCouponResponseDTO from(Coupon coupon) {
        return new RedeemCouponResponseDTO(
            coupon.getCode(),
            coupon.getStatus(),
            coupon.getRedeemedAt()
        );
    }
}
