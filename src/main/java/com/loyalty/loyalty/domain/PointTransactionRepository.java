package com.loyalty.loyalty.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PointTransactionRepository extends JpaRepository<PointTransaction, UUID> {
    Optional<PointTransaction> findByAccountIdAndIdempotencyKey(UUID accountId, String idempotencyKey);
    List<PointTransaction> findByAccountIdOrderByCreatedAtDesc(UUID accountId);
}
