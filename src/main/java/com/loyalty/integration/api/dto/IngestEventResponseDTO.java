package com.loyalty.integration.api.dto;

import java.time.Instant;
import java.util.UUID;

public record IngestEventResponseDTO(
    UUID inboxId,
    String status,
    Instant receivedAt
) {
    public static IngestEventResponseDTO accepted(UUID inboxId, Instant receivedAt) {
        return new IngestEventResponseDTO(inboxId, "ACCEPTED", receivedAt);
    }
}
