package com.loyalty.automation.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AutomationExecutionRepository extends JpaRepository<AutomationExecution, UUID> {
    Optional<AutomationExecution> findByAutomationIdAndIdempotencyKey(UUID automationId, String idempotencyKey);
}
