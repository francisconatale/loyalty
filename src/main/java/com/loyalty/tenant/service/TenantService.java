package com.loyalty.tenant.service;

import com.loyalty.shared.exception.ResourceNotFoundException;
import com.loyalty.tenant.domain.Tenant;
import com.loyalty.tenant.domain.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;

    @Transactional
    public Tenant createTenant(String name) {
        Tenant tenant = new Tenant(UUID.randomUUID(), name);
        return tenantRepository.save(tenant);
    }

    @Transactional(readOnly = true)
    public Tenant getTenant(UUID id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with ID: " + id));
    }

    @Transactional(readOnly = true)
    public Tenant getActiveTenant(UUID id) {
        Tenant tenant = getTenant(id);
        if (!tenant.isActive()) {
            throw new IllegalStateException("Tenant is not active: " + id);
        }
        return tenant;
    }
}
