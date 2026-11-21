package com.loyalty.customer.api.dto;

import com.loyalty.customer.domain.Customer;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CustomerResponseDTO(
    UUID id,
    UUID tenantId,
    String name,
    String email,
    String phone,
    LocalDate birthDate,
    Instant createdAt
) {
    public static CustomerResponseDTO from(Customer customer) {
        return new CustomerResponseDTO(
            customer.getId(),
            customer.getTenantId(),
            customer.getName(),
            customer.getEmail(),
            customer.getPhone(),
            customer.getBirthDate(),
            customer.getCreatedAt()
        );
    }
}
