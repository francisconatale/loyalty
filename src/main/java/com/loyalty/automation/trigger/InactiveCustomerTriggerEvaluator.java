package com.loyalty.automation.trigger;

import com.loyalty.automation.domain.Automation;
import com.loyalty.automation.domain.TriggerType;
import com.loyalty.customer.domain.CustomerStats;
import com.loyalty.customer.domain.CustomerStatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class InactiveCustomerTriggerEvaluator implements TriggerEvaluator {

    private final CustomerStatsRepository statsRepository;

    @Override
    public boolean supports(TriggerType triggerType) {
        return triggerType == TriggerType.CUSTOMER_INACTIVE;
    }

    @Override
    public List<UUID> evaluate(Automation automation) {
        Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);
        return statsRepository.findAll().stream()
                .filter(s -> s.getTenantId().equals(automation.getTenantId()))
                .filter(s -> s.getLastPurchaseAt() != null && s.getLastPurchaseAt().isBefore(thirtyDaysAgo))
                .map(CustomerStats::getCustomerId)
                .toList();
    }
}
