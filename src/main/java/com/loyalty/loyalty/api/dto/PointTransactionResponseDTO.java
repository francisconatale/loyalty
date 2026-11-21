package com.loyalty.loyalty.api.dto;

import com.loyalty.loyalty.domain.PointTransaction;
import com.loyalty.loyalty.domain.PointTransactionType;

import java.time.Instant;
import java.util.UUID;

public record PointTransactionResponseDTO(
    UUID id,
    UUID accountId,
    PointTransactionType type,
    long amount,
    UUID referenceId,
    String idempotencyKey,
    String description,
    Instant createdAt
) {
    public static PointTransactionResponseDTO from(PointTransaction tx) {
        return new PointTransactionResponseDTO(
            tx.getId(),
            tx.getAccountId(),
            tx.getType(),
            tx.getAmount(),
            tx.getReferenceId(),
            tx.getIdempotencyKey(),
            tx.getDescription(),
            tx.getCreatedAt()
        );
    }
}
