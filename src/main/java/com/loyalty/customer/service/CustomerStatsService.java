package com.loyalty.customer.service;

import com.loyalty.customer.domain.CustomerStats;
import com.loyalty.customer.domain.CustomerStatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerStatsService {

    private final CustomerStatsRepository statsRepository;

    @Transactional
    public CustomerStats recordPurchase(UUID tenantId, UUID customerId, BigDecimal amount, Instant purchasedAt) {
        CustomerStats stats = statsRepository.findById(customerId)
                .orElseGet(() -> new CustomerStats(customerId, tenantId));
        stats.recordPurchase(amount, purchasedAt);
        return statsRepository.save(stats);
    }

    @Transactional
    public void recordRefund(UUID customerId, BigDecimal amount) {
        statsRepository.findById(customerId).ifPresent(stats -> {
            stats.recordRefund(amount);
            statsRepository.save(stats);
        });
    }

    @Transactional(readOnly = true)
    public CustomerStats getStats(UUID customerId, UUID tenantId) {
        return statsRepository.findById(customerId)
                .orElseGet(() -> new CustomerStats(customerId, tenantId));
    }
}
