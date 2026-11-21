package com.loyalty.customer.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CustomerRequestDTO(
    @NotBlank(message = "Customer name is required")
    @Size(max = 128, message = "Customer name cannot exceed 128 characters")
    String name,

    @Email(message = "Email must be a valid email address")
    @Size(max = 256, message = "Email cannot exceed 256 characters")
    String email,

    @Size(max = 64, message = "Phone cannot exceed 64 characters")
    String phone,

    LocalDate birthDate
) {}
