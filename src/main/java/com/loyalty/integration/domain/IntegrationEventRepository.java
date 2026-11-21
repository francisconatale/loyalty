package com.loyalty.integration.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IntegrationEventRepository extends JpaRepository<IntegrationEvent, UUID> {
    Optional<IntegrationEvent> findByIntegrationIdAndExternalEventId(UUID integrationId, String externalEventId);
    List<IntegrationEvent> findTop50ByStatusOrderByReceivedAtAsc(EventStatus status);
}
