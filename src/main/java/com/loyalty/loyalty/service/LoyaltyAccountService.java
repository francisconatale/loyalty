package com.loyalty.loyalty.service;

import com.loyalty.loyalty.domain.*;
import com.loyalty.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LoyaltyAccountService {

    private final LoyaltyAccountRepository accountRepository;
    private final PointTransactionRepository transactionRepository;
    private final LoyaltyProgramService programService;

    @Transactional
    public LoyaltyAccount getOrCreateAccount(UUID tenantId, UUID customerId) {
        return accountRepository.findByTenantIdAndCustomerId(tenantId, customerId)
                .orElseGet(() -> {
                    LoyaltyProgram program = programService.createOrGetDefaultProgram(
                            tenantId, "Loyalty Points", "{\"pointsPerSpent\": 100}");
                    LoyaltyAccount account = new LoyaltyAccount(
                            UUID.randomUUID(), tenantId, customerId, program.getId());
                    return accountRepository.save(account);
                });
    }

    @Transactional
    public PointTransaction accruePoints(UUID tenantId, UUID customerId, long points,
                                         UUID referenceId, String idempotencyKey, String description) {
        LoyaltyAccount account = getOrCreateAccount(tenantId, customerId);

        Optional<PointTransaction> existing = transactionRepository
                .findByAccountIdAndIdempotencyKey(account.getId(), idempotencyKey);
        if (existing.isPresent()) {
            return existing.get();
        }

        account.accruePoints(points);
        accountRepository.save(account);

        PointTransaction tx = new PointTransaction(
                UUID.randomUUID(),
                account.getId(),
                PointTransactionType.EARN,
                points,
                referenceId,
                idempotencyKey,
                description
        );
        return transactionRepository.save(tx);
    }

    @Transactional
    public PointTransaction redeemPoints(UUID tenantId, UUID customerId, long points,
                                         UUID referenceId, String idempotencyKey, String description) {
        LoyaltyAccount account = getOrCreateAccount(tenantId, customerId);

        Optional<PointTransaction> existing = transactionRepository
                .findByAccountIdAndIdempotencyKey(account.getId(), idempotencyKey);
        if (existing.isPresent()) {
            return existing.get();
        }

        account.redeemPoints(points);
        accountRepository.save(account);

        PointTransaction tx = new PointTransaction(
                UUID.randomUUID(),
                account.getId(),
                PointTransactionType.REDEEM,
                -points,
                referenceId,
                idempotencyKey,
                description
        );
        return transactionRepository.save(tx);
    }

    @Transactional
    public PointTransaction reversePoints(UUID tenantId, UUID customerId, long points,
                                          UUID referenceId, String idempotencyKey, String description) {
        LoyaltyAccount account = getOrCreateAccount(tenantId, customerId);

        Optional<PointTransaction> existing = transactionRepository
                .findByAccountIdAndIdempotencyKey(account.getId(), idempotencyKey);
        if (existing.isPresent()) {
            return existing.get();
        }

        account.reversePoints(points);
        accountRepository.save(account);

        PointTransaction tx = new PointTransaction(
                UUID.randomUUID(),
                account.getId(),
                PointTransactionType.REVERSAL,
                -points,
                referenceId,
                idempotencyKey,
                description
        );
        return transactionRepository.save(tx);
    }

    @Transactional(readOnly = true)
    public LoyaltyAccount getAccount(UUID tenantId, UUID customerId) {
        return accountRepository.findByTenantIdAndCustomerId(tenantId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Loyalty account not found for customer: " + customerId));
    }

    @Transactional(readOnly = true)
    public List<PointTransaction> getTransactions(UUID accountId) {
        return transactionRepository.findByAccountIdOrderByCreatedAtDesc(accountId);
    }
}
