package com.loyalty.integration.adapters;

import com.loyalty.integration.api.dto.IngestEventRequestDTO;
import com.loyalty.purchase.domain.Purchase;

import java.util.UUID;

public interface PosNormalizationAdapter {
    String getProvider();
    Purchase normalizeAndProcess(UUID tenantId, UUID integrationId, IngestEventRequestDTO event);
}
