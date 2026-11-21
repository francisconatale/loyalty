package com.loyalty.integration.adapters.kioskito;

import com.loyalty.customer.domain.Customer;
import com.loyalty.customer.service.CustomerService;
import com.loyalty.integration.adapters.PosNormalizationAdapter;
import com.loyalty.integration.api.dto.IngestEventRequestDTO;
import com.loyalty.loyalty.service.LoyaltyAccountService;
import com.loyalty.purchase.domain.Purchase;
import com.loyalty.purchase.service.PurchaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class KioskitoNormalizationAdapter implements PosNormalizationAdapter {

    private final CustomerService customerService;
    private final PurchaseService purchaseService;
    private final LoyaltyAccountService loyaltyAccountService;

    @Override
    public String getProvider() {
        return "kioskito";
    }

    @Override
    public Purchase normalizeAndProcess(UUID tenantId, UUID integrationId, IngestEventRequestDTO event) {
        IngestEventRequestDTO.EventDataDTO data = event.data();

        // 1. Resolve canonical customer profile
        Customer customer = customerService.resolveOrCreateFromExternal(
                tenantId,
                integrationId,
                data.customerId(),
                data.customer()
        );

        // 2. Record normalized purchase
        Purchase purchase = purchaseService.recordPurchase(
                tenantId,
                integrationId,
                customer.getId(),
                data.purchaseId(),
                data.total(),
                data.currency(),
                event.occurredAt()
        );

        // 3. Accrue loyalty points: 1 point per 100 spent
        if (data.total() != null && data.total().compareTo(BigDecimal.ZERO) > 0) {
            long points = data.total().longValue() / 100;
            if (points > 0) {
                loyaltyAccountService.accruePoints(
                        tenantId,
                        customer.getId(),
                        points,
                        purchase.getId(),
                        "earn-" + purchase.getId(),
                        "Points earned on purchase " + data.purchaseId()
                );
            }
        }

        return purchase;
    }
}
