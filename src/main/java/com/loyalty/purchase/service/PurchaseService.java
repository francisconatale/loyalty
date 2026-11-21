package com.loyalty.purchase.service;

import com.loyalty.customer.service.CustomerStatsService;
import com.loyalty.purchase.domain.Purchase;
import com.loyalty.purchase.domain.PurchaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final CustomerStatsService customerStatsService;

    @Transactional
    public Purchase recordPurchase(UUID tenantId, UUID integrationId, UUID customerId,
                                   String externalPurchaseId, BigDecimal total, String currency,
                                   Instant purchasedAt) {
        Optional<Purchase> existing = purchaseRepository.findByIntegrationIdAndExternalId(integrationId, externalPurchaseId);
        if (existing.isPresent()) {
            return existing.get();
        }

        Purchase purchase = new Purchase(
                UUID.randomUUID(),
                tenantId,
                customerId,
                integrationId,
                externalPurchaseId,
                total,
                currency,
                purchasedAt
        );
        purchase = purchaseRepository.save(purchase);
        customerStatsService.recordPurchase(tenantId, customerId, total, purchasedAt);
        return purchase;
    }

    @Transactional
    public void handleRefundOrCancel(UUID integrationId, String externalPurchaseId, boolean isFullRefund) {
        purchaseRepository.findByIntegrationIdAndExternalId(integrationId, externalPurchaseId).ifPresent(purchase -> {
            if (isFullRefund) {
                purchase.refund();
            } else {
                purchase.cancel();
            }
            purchaseRepository.save(purchase);
            customerStatsService.recordRefund(purchase.getCustomerId(), purchase.getTotal());
        });
    }
}
