package com.loyalty.customer.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExternalCustomerReferenceRepository extends JpaRepository<ExternalCustomerReference, UUID> {
    Optional<ExternalCustomerReference> findByIntegrationIdAndExternalId(UUID integrationId, String externalId);
}
