package com.loyalty.loyalty.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LoyaltyProgramRepository extends JpaRepository<LoyaltyProgram, UUID> {
    Optional<LoyaltyProgram> findFirstByTenantIdAndEnabledTrue(UUID tenantId);
}
