package com.loyalty.reward.service;

import com.loyalty.coupon.domain.Coupon;
import com.loyalty.coupon.domain.DiscountType;
import com.loyalty.coupon.service.CouponService;
import com.loyalty.loyalty.service.LoyaltyAccountService;
import com.loyalty.reward.api.dto.RewardRequestDTO;
import com.loyalty.reward.domain.Reward;
import com.loyalty.reward.domain.RewardRepository;
import com.loyalty.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RewardService {

    private final RewardRepository rewardRepository;
    private final LoyaltyAccountService loyaltyAccountService;
    private final CouponService couponService;

    @Transactional
    public Reward createReward(UUID tenantId, RewardRequestDTO request) {
        Reward reward = new Reward(
                UUID.randomUUID(),
                tenantId,
                request.name(),
                request.description(),
                request.pointsCost()
        );
        return rewardRepository.save(reward);
    }

    @Transactional(readOnly = true)
    public List<Reward> listActiveRewards(UUID tenantId) {
        return rewardRepository.findByTenantIdAndActiveTrue(tenantId);
    }

    @Transactional
    public Coupon redeemRewardForCustomer(UUID tenantId, UUID customerId, UUID rewardId) {
        Reward reward = rewardRepository.findById(rewardId)
                .filter(Reward::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Active reward not found with ID: " + rewardId));

        // Deduct points from loyalty account
        loyaltyAccountService.redeemPoints(
                tenantId,
                customerId,
                reward.getPointsCost(),
                reward.getId(),
                "reward-redemption-" + UUID.randomUUID(),
                "Redeemed reward: " + reward.getName()
        );

        // Issue single-use coupon (valid for 30 days)
        Instant expiresAt = Instant.now().plus(30, ChronoUnit.DAYS);
        return couponService.generateCoupon(
                tenantId,
                customerId,
                reward.getId(),
                DiscountType.PERCENTAGE,
                BigDecimal.valueOf(15.00),
                expiresAt
        );
    }
}
