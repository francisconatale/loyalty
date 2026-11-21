package com.loyalty.customer.api.dto;

import com.loyalty.customer.domain.CustomerStats;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CustomerStatsResponseDTO(
    UUID customerId,
    UUID tenantId,
    int totalPurchases,
    BigDecimal totalSpent,
    BigDecimal averageTicket,
    Instant firstPurchaseAt,
    Instant lastPurchaseAt
) {
    public static CustomerStatsResponseDTO from(CustomerStats stats) {
        return new CustomerStatsResponseDTO(
            stats.getCustomerId(),
            stats.getTenantId(),
            stats.getTotalPurchases(),
            stats.getTotalSpent(),
            stats.getAverageTicket(),
            stats.getFirstPurchaseAt(),
            stats.getLastPurchaseAt()
        );
    }
}
