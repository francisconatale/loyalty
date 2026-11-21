package com.loyalty.automation.action;

import com.loyalty.automation.domain.ActionType;
import com.loyalty.automation.domain.Automation;
import com.loyalty.coupon.domain.Coupon;
import com.loyalty.coupon.domain.DiscountType;
import com.loyalty.coupon.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GenerateCouponActionExecutor implements ActionExecutor {

    private final CouponService couponService;

    @Override
    public boolean supports(ActionType actionType) {
        return actionType == ActionType.GENERATE_COUPON;
    }

    @Override
    public String execute(Automation automation, UUID customerId) {
        Instant expiresAt = Instant.now().plus(14, ChronoUnit.DAYS);
        Coupon coupon = couponService.generateCoupon(
                automation.getTenantId(),
                customerId,
                null,
                DiscountType.PERCENTAGE,
                BigDecimal.valueOf(20.00),
                expiresAt
        );
        return "{\"couponCode\":\"" + coupon.getCode() + "\",\"discountValue\":20.00}";
    }
}
