# Implementation Plan: Loyalty Engine Core MVP

**Branch**: `001-core-loyalty-mvp` | **Date**: 2026-11-21 | **Spec**: [spec.md](file:///home/francisco/loyalty-engine/specs/001-core-loyalty-mvp/spec.md)

**Input**: Feature specification from `/specs/001-core-loyalty-mvp/spec.md`

## Summary

Deliver the Core MVP of Loyalty Engine: a multi-tenant, SaaS-ready loyalty and customer automation platform integrating with external POS systems (initial canonical adapter: Kioskito). The implementation establishes:
1. Multi-tenant POS ingestion through a single `POST /api/v1/events` inbox (`IntegrationEvent`), with optional inline customer upsert (`ExternalCustomerReference`).
2. Idempotent purchase processing and real-time customer behavior analytics (`CustomerStats`).
3. Configurable loyalty programs with an immutable point ledger (`PointTransaction`) and zero negative-balance guarantees.
4. Reward catalog and single-use discount coupon issuance/validation/redemption (`Coupon`).
5. Event-driven automation rules engine (`AutomationExecution` / `AutomationAction`) with transactional email dispatch.

## Technical Context

**Language/Version**: Java 17 (Records, Pattern Matching, Sealed Types)

**Primary Dependencies**: Spring Boot 3.2.5, Spring Data JPA, Hibernate 6, Spring Security 6, Jakarta Validation, Springdoc OpenAPI 2.5.0, OpenPDF 1.3.30, Lombok

**Storage**: PostgreSQL 15+ with native JSONB mapping via Hibernate 6 `@JdbcTypeCode(SqlTypes.JSON)`

**Testing**: JUnit 5, AssertJ, Spring Boot Test, Spring Security Test, `@DataJpaTest` with real database semantics (H2 PostgreSQL mode / Testcontainers), `@WebMvcTest` / MockMvc

**Target Platform**: Linux server / Containerized Docker deployment

**Project Type**: Spring Boot Modular Monolith REST Backend / BFF

**Performance Goals**:
- Event ingestion persistence & acceptance response: <500ms p95
- End-to-end event-to-ledger processing: <5s
- Deduplication: 100% of duplicate events skipped without side effects

**Constraints**:
- Strict multi-tenant data isolation; tenant context bound at security boundary
- No class-level `@Setter` or `@Data` on JPA entities
- All REST DTOs declared as Java 17 `record` types
- Three isolated Spring Security filter chains; REST endpoints return HTTP 401 (never HTML 302 redirects)
- No in-memory mocking of internal JPA repositories in integration tests

**Scale/Scope**: Modular monolith with 11 domain modules, 15 core entities, supporting multi-tenant POS connections and automated re-engagement workflows.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Gate Requirement | Status | Design Evidence |
|---|---|---|---|
| **I. Zero-Boilerplate Immutability** | All DTOs must be Java 17 `record` with declarative Jakarta Validation and `RecordDTO.from(entity)` factory methods; no entities exposed in controllers. | ✅ PASSED | All contracts (`events-api-contract.json`, `coupons-api-contract.json`, `customer-loyalty-api-contract.json`) define record DTOs. Controllers never leak JPA entities. |
| **II. Rich Domain Model & Encapsulation** | JPA entities use `@Getter` only (no `@Data`, no class-level `@Setter`), domain constructors validate invariants, domain methods manage state transitions, `FetchType.LAZY` on associations, JSONB mapped via `@JdbcTypeCode(SqlTypes.JSON)`. | ✅ PASSED | Documented in `data-model.md`. Entities protect invariants in methods (`accrue()`, `redeem()`) and use explicit JSONB mappings. |
| **III. Fail-Fast Validation & Error Handling** | Controllers annotate `@Valid @RequestBody`, domain invariants throw runtime exceptions, `@RestControllerAdvice` emits standardized `ApiErrorResponse`. | ✅ PASSED | `GlobalExceptionHandler` emits `ApiErrorResponse` (`errorCode`, `message`, `path`, `timestamp`) and contracts document 400/401 responses. Fail-fast boundary enforced. |
| **IV. Multi-Tier Security Isolation** | 3 filter chains (`/api/v1/events` + `/api/v1/coupons/**` stateless API Key, `/api/v1/**` JSON login 401 entry point, `/**` portal). `@Service` decoupled from HTTP classes. | ✅ PASSED | Research document details the 3 ordered filter chains and `ApiKeyFilter` resolving tenant context. |
| **V. Realistic Persistence & Testing** | No mocking internal JPA repositories; `@DataJpaTest` with real database semantics; security tests verify 401 unauthenticated challenges. | ✅ PASSED | Quickstart guide and testing architecture forbid repo mocking and use `@DataJpaTest`. |

## Project Structure

### Documentation (this feature)

```text
specs/001-core-loyalty-mvp/
├── plan.md              # Implementation plan & architectural gates
├── research.md          # Technical decisions & design patterns
├── data-model.md        # Relational schema, entities, constraints
├── quickstart.md        # End-to-end runnable validation guide
├── contracts/           # External API OpenAPI contracts
│   ├── events-api-contract.json
│   ├── coupons-api-contract.json
│   └── customer-loyalty-api-contract.json
└── tasks.md             # Implementation tasks (/speckit-tasks output)
```

### Source Code (repository root)

```text
src/main/java/com/loyalty/
├── tenant/
│   ├── api/
│   │   ├── dto/
│   │   └── TenantController.java
│   ├── domain/
│   │   ├── Tenant.java
│   │   ├── TenantStatus.java
│   │   └── TenantRepository.java
│   └── service/
│       └── TenantService.java
│
├── integration/
│   ├── api/
│   │   ├── dto/
│   │   │   ├── IngestEventRequestDTO.java
│   │   │   └── IngestEventResponseDTO.java
│   │   └── EventIngestionController.java
│   ├── domain/
│   │   ├── PosIntegration.java
│   │   ├── IntegrationEvent.java
│   │   ├── EventStatus.java
│   │   ├── IntegrationEventRepository.java
│   │   └── PosIntegrationRepository.java
│   ├── service/
│   │   ├── IntegrationEventInboxService.java
│   │   └── PosIntegrationService.java
│   └── adapters/
│       ├── PosNormalizationAdapter.java
│       └── kioskito/
│           ├── KioskitoNormalizationAdapter.java
│           └── dto/
│
├── customer/
│   ├── api/
│   │   ├── dto/
│   │   │   ├── CustomerResponseDTO.java
│   │   │   └── CustomerStatsResponseDTO.java
│   │   └── CustomerController.java
│   ├── domain/
│   │   ├── Customer.java
│   │   ├── ExternalCustomerReference.java
│   │   ├── CustomerStats.java
│   │   ├── CustomerRepository.java
│   │   ├── ExternalCustomerReferenceRepository.java
│   │   └── CustomerStatsRepository.java
│   └── service/
│       ├── CustomerService.java
│       └── CustomerStatsService.java
│
├── purchase/
│   ├── domain/
│   │   ├── Purchase.java
│   │   ├── PurchaseStatus.java
│   │   └── PurchaseRepository.java
│   └── service/
│       └── PurchaseService.java
│
├── loyalty/
│   ├── api/
│   │   ├── dto/
│   │   │   ├── CustomerLoyaltyResponseDTO.java
│   │   │   └── PointTransactionResponseDTO.java
│   │   └── LoyaltyController.java
│   ├── domain/
│   │   ├── LoyaltyProgram.java
│   │   ├── LoyaltyAccount.java
│   │   ├── PointTransaction.java
│   │   ├── PointTransactionType.java
│   │   ├── LoyaltyProgramRepository.java
│   │   ├── LoyaltyAccountRepository.java
│   │   └── PointTransactionRepository.java
│   └── service/
│       ├── LoyaltyProgramService.java
│       └── LoyaltyAccountService.java
│
├── reward/
│   ├── api/
│   │   ├── dto/
│   │   │   ├── RewardRequestDTO.java
│   │   │   └── RewardResponseDTO.java
│   │   └── RewardController.java
│   ├── domain/
│   │   ├── Reward.java
│   │   └── RewardRepository.java
│   └── service/
│       └── RewardService.java
│
├── coupon/
│   ├── api/
│   │   ├── dto/
│   │   │   ├── ValidateCouponRequestDTO.java
│   │   │   ├── CouponValidationResponseDTO.java
│   │   │   ├── RedeemCouponRequestDTO.java
│   │   │   └── RedeemCouponResponseDTO.java
│   │   └── CouponController.java
│   ├── domain/
│   │   ├── Coupon.java
│   │   ├── CouponStatus.java
│   │   ├── DiscountType.java
│   │   └── CouponRepository.java
│   └── service/
│       └── CouponService.java
│
├── automation/
│   ├── domain/
│   │   ├── Automation.java
│   │   ├── AutomationExecution.java
│   │   ├── AutomationAction.java
│   │   ├── TriggerType.java
│   │   ├── ExecutionStatus.java
│   │   └── ActionType.java
│   ├── trigger/
│   │   ├── TriggerEvaluator.java
│   │   └── InactiveCustomerTriggerEvaluator.java
│   ├── action/
│   │   ├── ActionExecutor.java
│   │   ├── GenerateCouponActionExecutor.java
│   │   └── SendEmailActionExecutor.java
│   └── service/
│       └── AutomationEngineService.java
│
├── notification/
│   ├── email/
│   │   ├── EmailNotificationService.java
│   │   └── EmailProviderAdapter.java
│   └── whatsapp/
│       └── WhatsAppProviderAdapter.java
│
└── shared/
    ├── domain/
    │   └── BaseEntity.java
    ├── exception/
    │   ├── GlobalExceptionHandler.java
    │   ├── ApiErrorResponse.java
    │   ├── ResourceNotFoundException.java
    │   ├── InsufficientPointsException.java
    │   └── DuplicateEventException.java
    └── security/
        ├── SecurityConfig.java
        ├── ApiKeyFilter.java
        ├── JsonAuthenticationFilter.java
        ├── Roles.java
        └── TenantSecurityContext.java

src/main/resources/
├── application.yml
└── db/migration/
    └── V1__initial_schema.sql

src/test/java/com/loyalty/
├── integration/
│   ├── PosEventIngestionIntegrationTest.java
│   ├── LoyaltyLedgerIntegrationTest.java
│   └── CouponRedemptionIntegrationTest.java
└── security/
    └── MultiChainSecurityTest.java
```

**Structure Decision**: Standard Maven multi-package modular monolith under `com.loyalty` conforming to the package architecture prescribed in the functional specification and constitution.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|---|---|---|
| None | N/A | Fully adheres to Constitution principles. |
