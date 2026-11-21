package com.loyalty.reward.api;

import com.loyalty.coupon.api.dto.CouponValidationResponseDTO;
import com.loyalty.coupon.domain.Coupon;
import com.loyalty.reward.api.dto.RewardRequestDTO;
import com.loyalty.reward.api.dto.RewardResponseDTO;
import com.loyalty.reward.domain.Reward;
import com.loyalty.reward.service.RewardService;
import com.loyalty.shared.security.TenantSecurityContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rewards")
@RequiredArgsConstructor
public class RewardController {

    private final RewardService rewardService;

    @PostMapping
    public ResponseEntity<RewardResponseDTO> createReward(@Valid @RequestBody RewardRequestDTO request) {
        UUID tenantId = TenantSecurityContext.getTenantId();
        Reward reward = rewardService.createReward(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(RewardResponseDTO.from(reward));
    }

    @GetMapping
    public ResponseEntity<List<RewardResponseDTO>> listRewards() {
        UUID tenantId = TenantSecurityContext.getTenantId();
        List<RewardResponseDTO> list = rewardService.listActiveRewards(tenantId).stream()
                .map(RewardResponseDTO::from)
                .toList();
        return ResponseEntity.ok(list);
    }

    @PostMapping("/{rewardId}/redeem")
    public ResponseEntity<CouponValidationResponseDTO> redeemReward(
            @PathVariable("rewardId") UUID rewardId,
            @RequestParam("customerId") UUID customerId) {
        UUID tenantId = TenantSecurityContext.getTenantId();
        Coupon coupon = rewardService.redeemRewardForCustomer(tenantId, customerId, rewardId);
        return ResponseEntity.ok(CouponValidationResponseDTO.from(coupon));
    }
}
