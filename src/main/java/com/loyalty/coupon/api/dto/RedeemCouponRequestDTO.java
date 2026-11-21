package com.loyalty.coupon.api.dto;

import jakarta.validation.constraints.NotBlank;

public record RedeemCouponRequestDTO(
    @NotBlank(message = "Coupon code is required")
    String code,

    @NotBlank(message = "Transaction reference is required")
    String transactionReference
) {}
