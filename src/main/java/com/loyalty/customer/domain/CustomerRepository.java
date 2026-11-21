package com.loyalty.customer.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    Optional<Customer> findByTenantIdAndId(UUID tenantId, UUID id);
    Optional<Customer> findByTenantIdAndEmail(UUID tenantId, String email);
}
