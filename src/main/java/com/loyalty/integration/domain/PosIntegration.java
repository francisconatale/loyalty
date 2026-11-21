package com.loyalty.integration.domain;

import com.loyalty.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "pos_integrations", uniqueConstraints = {
    @UniqueConstraint(name = "uq_pos_integrations_provider_store", columnNames = {"provider", "external_store_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PosIntegration extends BaseEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "provider", nullable = false, length = 64)
    private String provider;

    @Column(name = "external_store_id", nullable = false, length = 128)
    private String externalStoreId;

    @Column(name = "api_key_hash", nullable = false, length = 256)
    private String apiKeyHash;

    @Column(name = "encrypted_credentials", nullable = false, columnDefinition = "TEXT")
    private String encryptedCredentials;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private IntegrationStatus status;

    public PosIntegration(UUID id, UUID tenantId, String provider, String externalStoreId,
                          String apiKeyHash, String encryptedCredentials) {
        this.id = Objects.requireNonNull(id, "Integration ID cannot be null");
        this.tenantId = Objects.requireNonNull(tenantId, "Tenant ID cannot be null");
        this.provider = Objects.requireNonNull(provider, "Provider cannot be null");
        this.externalStoreId = Objects.requireNonNull(externalStoreId, "External store ID cannot be null");
        this.apiKeyHash = Objects.requireNonNull(apiKeyHash, "API Key hash cannot be null");
        this.encryptedCredentials = Objects.requireNonNull(encryptedCredentials, "Encrypted credentials cannot be null");
        this.status = IntegrationStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = IntegrationStatus.DISCONNECTED;
    }

    public void markError() {
        this.status = IntegrationStatus.ERROR;
    }

    public boolean isActive() {
        return this.status == IntegrationStatus.ACTIVE;
    }
}
