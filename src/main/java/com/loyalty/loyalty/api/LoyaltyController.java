package com.loyalty.loyalty.api;

import com.loyalty.loyalty.api.dto.CustomerLoyaltyResponseDTO;
import com.loyalty.loyalty.api.dto.PointTransactionResponseDTO;
import com.loyalty.loyalty.domain.LoyaltyAccount;
import com.loyalty.loyalty.service.LoyaltyAccountService;
import com.loyalty.shared.security.TenantSecurityContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class LoyaltyController {

    private final LoyaltyAccountService loyaltyAccountService;

    @GetMapping("/{customerId}/loyalty")
    public ResponseEntity<CustomerLoyaltyResponseDTO> getCustomerLoyalty(
            @PathVariable("customerId") UUID customerId) {
        UUID tenantId = TenantSecurityContext.getTenantId();
        LoyaltyAccount account = loyaltyAccountService.getOrCreateAccount(tenantId, customerId);
        return ResponseEntity.ok(CustomerLoyaltyResponseDTO.from(account));
    }

    @GetMapping("/{customerId}/transactions")
    public ResponseEntity<List<PointTransactionResponseDTO>> getTransactions(
            @PathVariable("customerId") UUID customerId) {
        UUID tenantId = TenantSecurityContext.getTenantId();
        LoyaltyAccount account = loyaltyAccountService.getOrCreateAccount(tenantId, customerId);
        List<PointTransactionResponseDTO> transactions = loyaltyAccountService.getTransactions(account.getId())
                .stream()
                .map(PointTransactionResponseDTO::from)
                .toList();
        return ResponseEntity.ok(transactions);
    }
}
