package com.loyalty.customer.domain;

import com.loyalty.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "external_customer_references", uniqueConstraints = {
    @UniqueConstraint(name = "uq_ext_cust_ref_integration_external", columnNames = {"integration_id", "external_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExternalCustomerReference extends BaseEntity {

    @Id
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "integration_id", nullable = false)
    private UUID integrationId;

    @Column(name = "external_id", nullable = false, length = 128)
    private String externalId;

    public ExternalCustomerReference(UUID id, UUID customerId, UUID integrationId, String externalId) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.customerId = Objects.requireNonNull(customerId, "Customer ID cannot be null");
        this.integrationId = Objects.requireNonNull(integrationId, "Integration ID cannot be null");
        this.externalId = Objects.requireNonNull(externalId, "External ID cannot be null");
    }
}
