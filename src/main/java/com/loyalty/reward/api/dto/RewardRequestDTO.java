package com.loyalty.reward.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record RewardRequestDTO(
    @NotBlank(message = "Reward name is required")
    @Size(max = 128, message = "Reward name cannot exceed 128 characters")
    String name,

    String description,

    @Positive(message = "Points cost must be positive")
    long pointsCost
) {}
