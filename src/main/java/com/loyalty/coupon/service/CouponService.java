package com.loyalty.coupon.service;

import com.loyalty.coupon.api.dto.CouponValidationResponseDTO;
import com.loyalty.coupon.api.dto.RedeemCouponResponseDTO;
import com.loyalty.coupon.domain.Coupon;
import com.loyalty.coupon.domain.CouponRepository;
import com.loyalty.coupon.domain.DiscountType;
import com.loyalty.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;

    @Transactional(readOnly = true)
    public CouponValidationResponseDTO validateCoupon(UUID tenantId, String code) {
        Coupon coupon = couponRepository.findByTenantIdAndCode(tenantId, code.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found with code: " + code));
        return CouponValidationResponseDTO.from(coupon);
    }

    @Transactional
    public RedeemCouponResponseDTO redeemCoupon(UUID tenantId, String code, String transactionReference) {
        Coupon coupon = couponRepository.findByTenantIdAndCode(tenantId, code.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found with code: " + code));

        coupon.redeem();
        coupon = couponRepository.save(coupon);
        return RedeemCouponResponseDTO.from(coupon);
    }

    @Transactional
    public Coupon generateCoupon(UUID tenantId, UUID customerId, UUID rewardId,
                                 DiscountType discountType, BigDecimal discountValue,
                                 Instant expiresAt) {
        String randomSuffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String code = "COUPON-" + randomSuffix;

        Coupon coupon = new Coupon(
                UUID.randomUUID(),
                tenantId,
                customerId,
                rewardId,
                code,
                discountType,
                discountValue,
                expiresAt
        );
        return couponRepository.save(coupon);
    }
}
