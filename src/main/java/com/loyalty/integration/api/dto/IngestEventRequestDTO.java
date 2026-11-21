package com.loyalty.integration.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.loyalty.customer.api.dto.CustomerRequestDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public record IngestEventRequestDTO(
    @NotBlank(message = "Event type is required")
    String type,

    @NotBlank(message = "Event ID is required")
    String eventId,

    @NotNull(message = "occurredAt timestamp is required")
    Instant occurredAt,

    @NotNull(message = "Event data payload is required")
    EventDataDTO data
) {
    public record EventDataDTO(
        @NotBlank(message = "Purchase ID is required for purchase events")
        String purchaseId,

        @NotBlank(message = "Customer ID is required")
        String customerId,

        @NotNull(message = "Total is required")
        BigDecimal total,

        String currency,

        CustomerRequestDTO customer
    ) {}
}
