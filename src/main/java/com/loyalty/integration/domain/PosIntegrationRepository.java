package com.loyalty.integration.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PosIntegrationRepository extends JpaRepository<PosIntegration, UUID> {
    Optional<PosIntegration> findByApiKeyHash(String apiKeyHash);
    Optional<PosIntegration> findByProviderAndExternalStoreId(String provider, String externalStoreId);
}
