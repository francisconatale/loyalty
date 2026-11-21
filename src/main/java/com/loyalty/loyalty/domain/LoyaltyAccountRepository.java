package com.loyalty.loyalty.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LoyaltyAccountRepository extends JpaRepository<LoyaltyAccount, UUID> {
    Optional<LoyaltyAccount> findByTenantIdAndCustomerId(UUID tenantId, UUID customerId);
    Optional<LoyaltyAccount> findByProgramIdAndCustomerId(UUID programId, UUID customerId);
}
