package com.loyalty.customer.domain;

import com.loyalty.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "customers", uniqueConstraints = {
    @UniqueConstraint(name = "uq_customers_tenant_email", columnNames = {"tenant_id", "email"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Customer extends BaseEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "email", length = 256)
    private String email;

    @Column(name = "phone", length = 64)
    private String phone;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    public Customer(UUID id, UUID tenantId, String name, String email, String phone, LocalDate birthDate) {
        this.id = Objects.requireNonNull(id, "Customer ID cannot be null");
        this.tenantId = Objects.requireNonNull(tenantId, "Tenant ID cannot be null");
        this.name = Objects.requireNonNull(name, "Customer name cannot be null");
        this.email = email != null && !email.isBlank() ? email.trim().toLowerCase() : null;
        this.phone = phone != null && !phone.isBlank() ? phone.trim() : null;
        this.birthDate = birthDate;
    }

    public void updateProfile(String name, String email, String phone, LocalDate birthDate) {
        if (name != null && !name.isBlank()) {
            this.name = name.trim();
        }
        if (email != null && !email.isBlank()) {
            this.email = email.trim().toLowerCase();
        }
        if (phone != null && !phone.isBlank()) {
            this.phone = phone.trim();
        }
        if (birthDate != null) {
            this.birthDate = birthDate;
        }
    }
}
