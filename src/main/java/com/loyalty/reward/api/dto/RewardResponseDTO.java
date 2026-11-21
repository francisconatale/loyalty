package com.loyalty.reward.api.dto;

import com.loyalty.reward.domain.Reward;

import java.util.UUID;

public record RewardResponseDTO(
    UUID id,
    String name,
    String description,
    long pointsCost,
    boolean active
) {
    public static RewardResponseDTO from(Reward reward) {
        return new RewardResponseDTO(
            reward.getId(),
            reward.getName(),
            reward.getDescription(),
            reward.getPointsCost(),
            reward.isActive()
        );
    }
}
