package com.loyalty.integration.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyalty.integration.adapters.PosNormalizationAdapter;
import com.loyalty.integration.api.dto.IngestEventRequestDTO;
import com.loyalty.integration.api.dto.IngestEventResponseDTO;
import com.loyalty.integration.domain.EventStatus;
import com.loyalty.integration.domain.IntegrationEvent;
import com.loyalty.integration.domain.IntegrationEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationEventInboxService {

    private final IntegrationEventRepository eventRepository;
    private final List<PosNormalizationAdapter> normalizationAdapters;
    private final ObjectMapper objectMapper;

    @Transactional
    public IngestEventResponseDTO ingest(UUID tenantId, UUID integrationId, IngestEventRequestDTO request) {
        Optional<IntegrationEvent> existing = eventRepository.findByIntegrationIdAndExternalEventId(
                integrationId, request.eventId());

        if (existing.isPresent()) {
            IntegrationEvent event = existing.get();
            return IngestEventResponseDTO.accepted(event.getId(), event.getReceivedAt());
        }

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot serialize event payload", e);
        }

        IntegrationEvent event = new IntegrationEvent(
                UUID.randomUUID(),
                tenantId,
                integrationId,
                request.eventId(),
                request.type(),
                payloadJson
        );
        event = eventRepository.save(event);

        // Immediate dispatch for MVP responsive SLA
        processEvent(event, request);

        return IngestEventResponseDTO.accepted(event.getId(), event.getReceivedAt());
    }

    public void processEvent(IntegrationEvent event, IngestEventRequestDTO request) {
        try {
            event.markProcessing();
            eventRepository.save(event);

            // Canonical adapter resolution
            PosNormalizationAdapter adapter = normalizationAdapters.stream()
                    .filter(a -> a.getProvider().equalsIgnoreCase("kioskito"))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No POS normalization adapter configured"));

            adapter.normalizeAndProcess(event.getTenantId(), event.getIntegrationId(), request);

            event.markProcessed();
            eventRepository.save(event);
        } catch (Exception e) {
            log.error("Failed to process event {}", event.getId(), e);
            event.markFailed();
            eventRepository.save(event);
        }
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void retryPendingEvents() {
        List<IntegrationEvent> pending = eventRepository.findTop50ByStatusOrderByReceivedAtAsc(EventStatus.PENDING);
        for (IntegrationEvent event : pending) {
            try {
                IngestEventRequestDTO request = objectMapper.readValue(event.getPayload(), IngestEventRequestDTO.class);
                processEvent(event, request);
            } catch (Exception e) {
                log.error("Error retrying pending event {}", event.getId(), e);
                event.markFailed();
                eventRepository.save(event);
            }
        }
    }
}
