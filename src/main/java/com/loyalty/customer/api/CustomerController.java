package com.loyalty.customer.api;

import com.loyalty.customer.api.dto.CustomerResponseDTO;
import com.loyalty.customer.domain.Customer;
import com.loyalty.customer.service.CustomerService;
import com.loyalty.shared.security.TenantSecurityContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponseDTO> getCustomer(@PathVariable("id") UUID id) {
        UUID tenantId = TenantSecurityContext.getTenantId();
        Customer customer = customerService.getCustomer(tenantId, id);
        return ResponseEntity.ok(CustomerResponseDTO.from(customer));
    }
}
