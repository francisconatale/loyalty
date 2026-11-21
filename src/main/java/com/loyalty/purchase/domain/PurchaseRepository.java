package com.loyalty.purchase.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, UUID> {
    Optional<Purchase> findByIntegrationIdAndExternalId(UUID integrationId, String externalId);
}
