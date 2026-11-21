package com.loyalty.integration.adapters.kioskito;

import com.loyalty.customer.domain.Customer;
import com.loyalty.customer.service.CustomerService;
import com.loyalty.integration.adapters.PosNormalizationAdapter;
import com.loyalty.integration.api.dto.IngestEventRequestDTO;
import com.loyalty.purchase.domain.Purchase;
import com.loyalty.purchase.service.PurchaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class KioskitoNormalizationAdapter implements PosNormalizationAdapter {

    private final CustomerService customerService;
    private final PurchaseService purchaseService;

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
        return purchaseService.recordPurchase(
                tenantId,
                integrationId,
                customer.getId(),
                data.purchaseId(),
                data.total(),
                data.currency(),
                event.occurredAt()
        );
    }
}
