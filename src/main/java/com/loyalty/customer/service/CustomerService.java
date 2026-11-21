package com.loyalty.customer.service;

import com.loyalty.customer.api.dto.CustomerRequestDTO;
import com.loyalty.customer.domain.Customer;
import com.loyalty.customer.domain.CustomerRepository;
import com.loyalty.customer.domain.ExternalCustomerReference;
import com.loyalty.customer.domain.ExternalCustomerReferenceRepository;
import com.loyalty.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final ExternalCustomerReferenceRepository externalReferenceRepository;

    @Transactional
    public Customer resolveOrCreateFromExternal(UUID tenantId, UUID integrationId,
                                                String externalCustomerId, CustomerRequestDTO profile) {
        Optional<ExternalCustomerReference> refOpt = externalReferenceRepository
                .findByIntegrationIdAndExternalId(integrationId, externalCustomerId);

        if (refOpt.isPresent()) {
            Customer customer = customerRepository.findById(refOpt.get().getCustomerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Linked customer profile not found"));

            if (profile != null) {
                customer.updateProfile(profile.name(), profile.email(), profile.phone(), profile.birthDate());
                return customerRepository.save(customer);
            }
            return customer;
        }

        // Check if customer exists by tenant + email
        Customer customer;
        if (profile != null && profile.email() != null && !profile.email().isBlank()) {
            Optional<Customer> existingByEmail = customerRepository.findByTenantIdAndEmail(tenantId, profile.email().trim().toLowerCase());
            if (existingByEmail.isPresent()) {
                customer = existingByEmail.get();
                customer.updateProfile(profile.name(), profile.email(), profile.phone(), profile.birthDate());
                customer = customerRepository.save(customer);
            } else {
                customer = new Customer(
                        UUID.randomUUID(),
                        tenantId,
                        profile.name() != null ? profile.name() : "Customer " + externalCustomerId,
                        profile.email(),
                        profile.phone(),
                        profile.birthDate()
                );
                customer = customerRepository.save(customer);
            }
        } else {
            String customerName = (profile != null && profile.name() != null) ? profile.name() : "Customer " + externalCustomerId;
            String phone = profile != null ? profile.phone() : null;
            customer = new Customer(UUID.randomUUID(), tenantId, customerName, null, phone, profile != null ? profile.birthDate() : null);
            customer = customerRepository.save(customer);
        }

        ExternalCustomerReference newRef = new ExternalCustomerReference(
                UUID.randomUUID(),
                customer.getId(),
                integrationId,
                externalCustomerId
        );
        externalReferenceRepository.save(newRef);

        return customer;
    }

    @Transactional(readOnly = true)
    public Customer getCustomer(UUID tenantId, UUID customerId) {
        return customerRepository.findByTenantIdAndId(tenantId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + customerId));
    }
}
