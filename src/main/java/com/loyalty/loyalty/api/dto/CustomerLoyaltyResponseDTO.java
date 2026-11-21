package com.loyalty.loyalty.api.dto;

import com.loyalty.loyalty.domain.LoyaltyAccount;

import java.util.UUID;

public record CustomerLoyaltyResponseDTO(
    UUID accountId,
    UUID customerId,
    UUID programId,
    long pointsBalance,
    long lifetimePoints
) {
    public static CustomerLoyaltyResponseDTO from(LoyaltyAccount account) {
        return new CustomerLoyaltyResponseDTO(
            account.getId(),
            account.getCustomerId(),
            account.getProgramId(),
            account.getPointsBalance(),
            account.getLifetimePoints()
        );
    }
}
