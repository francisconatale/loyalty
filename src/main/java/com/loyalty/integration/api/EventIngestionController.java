package com.loyalty.integration.api;

import com.loyalty.integration.api.dto.IngestEventRequestDTO;
import com.loyalty.integration.api.dto.IngestEventResponseDTO;
import com.loyalty.integration.service.IntegrationEventInboxService;
import com.loyalty.shared.security.TenantSecurityContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventIngestionController {

    private final IntegrationEventInboxService inboxService;

    @PostMapping
    public ResponseEntity<IngestEventResponseDTO> ingestEvent(
            @Valid @RequestBody IngestEventRequestDTO request,
            Authentication authentication) {
        UUID tenantId = TenantSecurityContext.getTenantId();
        UUID integrationId = UUID.fromString(authentication.getName());

        IngestEventResponseDTO response = inboxService.ingest(tenantId, integrationId, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}
