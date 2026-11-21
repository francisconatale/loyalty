package com.loyalty.coupon.api.dto;

import jakarta.validation.constraints.NotBlank;

public record ValidateCouponRequestDTO(
    @NotBlank(message = "Coupon code is required")
    String code
) {}
