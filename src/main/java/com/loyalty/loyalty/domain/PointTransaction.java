package com.loyalty.loyalty.domain;

import com.loyalty.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "point_transactions", uniqueConstraints = {
    @UniqueConstraint(name = "uq_point_tx_account_idempotency", columnNames = {"account_id", "idempotency_key"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointTransaction extends BaseEntity {

    @Id
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private PointTransactionType type;

    @Column(name = "amount", nullable = false)
    private long amount;

    @Column(name = "reference_id")
    private UUID referenceId;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(name = "description", nullable = false)
    private String description;

    public PointTransaction(UUID id, UUID accountId, PointTransactionType type, long amount,
                            UUID referenceId, String idempotencyKey, String description) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.accountId = Objects.requireNonNull(accountId, "Account ID cannot be null");
        this.type = Objects.requireNonNull(type, "Type cannot be null");
        this.amount = amount;
        this.referenceId = referenceId;
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "Idempotency key cannot be null");
        this.description = description != null ? description : "";
    }
}
