package com.loyalty.coupon.api;

import com.loyalty.coupon.api.dto.CouponValidationResponseDTO;
import com.loyalty.coupon.api.dto.RedeemCouponRequestDTO;
import com.loyalty.coupon.api.dto.RedeemCouponResponseDTO;
import com.loyalty.coupon.api.dto.ValidateCouponRequestDTO;
import com.loyalty.coupon.service.CouponService;
import com.loyalty.shared.security.TenantSecurityContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @PostMapping("/validate")
    public ResponseEntity<CouponValidationResponseDTO> validateCoupon(
            @Valid @RequestBody ValidateCouponRequestDTO request) {
        UUID tenantId = TenantSecurityContext.getTenantId();
        CouponValidationResponseDTO response = couponService.validateCoupon(tenantId, request.code());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/redeem")
    public ResponseEntity<RedeemCouponResponseDTO> redeemCoupon(
            @Valid @RequestBody RedeemCouponRequestDTO request) {
        UUID tenantId = TenantSecurityContext.getTenantId();
        RedeemCouponResponseDTO response = couponService.redeemCoupon(tenantId, request.code(), request.transactionReference());
        return ResponseEntity.ok(response);
    }
}
