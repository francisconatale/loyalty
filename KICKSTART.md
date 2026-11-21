# KICKSTART.md — Loyalty Engine Developer Guide & Architecture Blueprint

Welcome to the **Loyalty Engine** project.
This document outlines the core architecture, data model patterns, zero-boilerplate implementation standards, and development workflows.

---

## 1. System Overview

`loyalty-engine` is a scalable loyalty and rewards platform built on **Spring Boot 3.2.5** and **Java 17**.

### Core Domain Model
- **Customer**: Profile owning one or more loyalty accounts.
- **LoyaltyAccount**: Main aggregate root managing active balance, tier level, and transactions.
- **PointTransaction**: Immutable ledger entries tracking accruals, redemptions, expirations, and adjustments.
- **Tier**: Customer loyalty levels (Bronze, Silver, Gold, Platinum) with benefits and point multipliers.
- **Reward**: Catalog items and vouchers redeemable with points.

```
       ┌────────────────────────┐
       │        Customer        │
       └───────────┬────────────┘
                   │ 1
                   │ owns
                   ▼ *
       ┌────────────────────────┐         * ┌────────────────────────┐
       │     LoyaltyAccount     ├───────────┤    PointTransaction    │
       └───────────┬────────────┘           └────────────────────────┘
                   │ *
                   │ belongs to
                   ▼ 1
       ┌────────────────────────┐
       │          Tier          │
       └────────────────────────┘
```

---

## 2. Technology Stack

All dependencies are defined in `pom.xml`:

| Technology | Purpose |
|---|---|
| **Spring Boot 3.2.5** | Core framework |
| **Java 17** | Records, pattern matching, sealed types |
| **PostgreSQL** | Primary relational database with JSONB support |
| **Spring Data JPA / Hibernate 6** | Repository layer & ORM |
| **Spring Security 6** | Multi-chain authentication (API Key + JSON REST Login) |
| **Jakarta Bean Validation** | Declarative fail-fast validations directly on records |
| **Lombok** | Boilerplate reduction for entities (`@Getter`) and services (`@RequiredArgsConstructor`) |
| **OpenPDF** | Statements and voucher generation |
| **Springdoc OpenAPI 3** | Swagger UI on `/swagger-ui.html` |

---

## 3. The Zero-Boilerplate Feature Recipe

When building or extending any feature, follow this exact blueprint:

### Step 1: Request & Response DTOs as Java 17 Records
Never create mutable classes with getters and setters for DTOs. Use `record` with declarative validation.

```java
// Command DTO: Accrue Points
public record AccruePointsRequestDTO(
    @NotNull(message = "Account ID is required")
    Long accountId,

    @NotNull(message = "Amount is required")
    @Positive(message = "Points to accrue must be strictly positive")
    Integer points,

    @NotBlank(message = "Transaction reference is required")
    @Size(max = 64, message = "Reference must not exceed 64 characters")
    String transactionReference,

    @Size(max = 255, message = "Reason cannot exceed 255 characters")
    String reason
) {}

// Response DTO: Transaction Statement
public record PointTransactionResponseDTO(
    Long id,
    Long accountId,
    Integer points,
    String type,
    String reference,
    Instant timestamp
) {
    public static PointTransactionResponseDTO from(PointTransaction tx) {
        return new PointTransactionResponseDTO(
            tx.getId(),
            tx.getAccount().getId(),
            tx.getPoints(),
            tx.getType().name(),
            tx.getReference(),
            tx.getCreatedAt()
        );
    }
}
```

### Step 2: JPA Aggregate Root Entity
- Use `@Getter` at the class level. Never use `@Data` or `@Setter`.
- Maintain state transitions through explicit domain methods.
- Enforce invariants inside constructor and methods.

```java
@Entity
@Table(name = "loyalty_accounts")
@Getter
public class LoyaltyAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false, unique = true)
    private String accountNumber;

    @Column(nullable = false)
    private Integer currentBalance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PointTransaction> transactions = new ArrayList<>();

    protected LoyaltyAccount() {}

    public LoyaltyAccount(String accountNumber, Customer customer) {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("Account number cannot be blank");
        }
        if (customer == null) {
            throw new IllegalArgumentException("Customer cannot be null");
        }
        this.accountNumber = accountNumber;
        this.customer = customer;
        this.currentBalance = 0;
    }

    public PointTransaction accrue(int points, String reference, String reason) {
        if (points <= 0) {
            throw new IllegalArgumentException("Accrual points must be greater than zero");
        }
        this.currentBalance += points;
        PointTransaction tx = new PointTransaction(this, points, TransactionType.ACCRUAL, reference, reason);
        this.transactions.add(tx);
        return tx;
    }

    public PointTransaction redeem(int points, String reference, String reason) {
        if (points <= 0) {
            throw new IllegalArgumentException("Redemption points must be greater than zero");
        }
        if (this.currentBalance < points) {
            throw new InsufficientPointsException("Insufficient balance: " + this.currentBalance + " available, requested: " + points);
        }
        this.currentBalance -= points;
        PointTransaction tx = new PointTransaction(this, -points, TransactionType.REDEMPTION, reference, reason);
        this.transactions.add(tx);
        return tx;
    }
}
```

### Step 3: Service Layer
Constructor injection via Lombok `@RequiredArgsConstructor`, pure business methods, zero references to Servlets or HTTP classes.

```java
@Service
@RequiredArgsConstructor
public class LoyaltyService {

    private final LoyaltyAccountRepository accountRepository;

    @Transactional
    public PointTransactionResponseDTO accruePoints(AccruePointsRequestDTO request) {
        LoyaltyAccount account = accountRepository.findById(request.accountId())
            .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + request.accountId()));

        PointTransaction tx = account.accrue(request.points(), request.transactionReference(), request.reason());
        return PointTransactionResponseDTO.from(tx);
    }
}
```

### Step 4: Controller Boundary
Annotate with `@Valid @RequestBody`, return `ResponseEntity`, and tag with OpenAPI annotations.

```java
@Tag(name = "Points", description = "Endpoints for point accruals and redemptions")
@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
public class PointTransactionController {

    private final LoyaltyService loyaltyService;

    @Operation(summary = "Accrue points to loyalty account")
    @PostMapping("/accrue")
    public ResponseEntity<PointTransactionResponseDTO> accrue(
            @Valid @RequestBody AccruePointsRequestDTO request) {
        PointTransactionResponseDTO response = loyaltyService.accruePoints(request);
        return ResponseEntity.ok(response);
    }
}
```

---

## 4. Security Architecture

1. **POS / Checkout M2M Integration (`@Order(1)`)**:
   - Path: `/api/v1/events`, `/api/v1/coupons/**`
   - Header: `X-API-KEY`
   - Stateless session policy.
2. **REST Application API (`@Order(2)`)**:
   - Path: `/api/**`
   - Login: `POST /api/auth/login` (JSON body handled by `JsonAuthenticationFilter`).
   - Entry point: `HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)` (returns 401, never redirects).
3. **Portal / Backoffice (`@Order(3)`)**:
   - Path: `/**`
   - Form Login with `/login`.

---

## 5. Anti-Patterns Checklist

- ❌ **No `@Data` on JPA Entities**: Causes `StackOverflowError` on lazy associations.
- ❌ **No Class-Level `@Setter` on Entities**: Leads to anemic models that bypass business invariants.
- ❌ **No Returning Entities directly from Controllers**: Exposes DB schema and causes `LazyInitializationException`.
- ❌ **No Manual Getters/Setters on DTOs**: Use Java 17 `record`.
- ❌ **No Eager Fetching**: Always use `FetchType.LAZY`.
- ❌ **No Servlet Classes in Services**: Keep `@Service` pure and easily testable.
