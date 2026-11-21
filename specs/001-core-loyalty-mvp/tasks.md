# Tasks: Loyalty Engine Core MVP

**Input**: Design documents from `/specs/001-core-loyalty-mvp/`
**Prerequisites**: [plan.md](file:///home/francisco/loyalty-engine/specs/001-core-loyalty-mvp/plan.md), [spec.md](file:///home/francisco/loyalty-engine/specs/001-core-loyalty-mvp/spec.md), [research.md](file:///home/francisco/loyalty-engine/specs/001-core-loyalty-mvp/research.md), [data-model.md](file:///home/francisco/loyalty-engine/specs/001-core-loyalty-mvp/data-model.md), [contracts/](file:///home/francisco/loyalty-engine/specs/001-core-loyalty-mvp/contracts/)

## Organization

Tasks are grouped by user story to enable independent implementation and testing of each story, preceded by setup and foundational infrastructure, and completed by cross-cutting concerns.

## Format: `- [ ] [TaskID] [P?] [Story?] Description with file path`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to ([US1], [US2], [US3], [US4], [US5])
- All tasks specify exact file paths

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization, directory structure, dependency verification, and base configuration.

- [X] T001 Create modular directory structure under src/main/java/com/loyalty and src/test/java/com/loyalty per implementation plan
- [X] T002 Verify and configure Maven build dependencies and compiler settings in pom.xml
- [X] T003 [P] Configure application runtime properties and PostgreSQL Hibernate JSONB settings in src/main/resources/application.yml
- [X] T004 [P] Configure test application properties with PostgreSQL compatibility mode in src/test/resources/application-test.yml

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure, shared error handling, multi-chain security, and database baseline that MUST be complete before ANY user story can be implemented.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T005 [P] Create BaseEntity with audit timestamps in src/main/java/com/loyalty/shared/domain/BaseEntity.java
- [X] T006 [P] Create ApiErrorResponse record DTO in src/main/java/com/loyalty/shared/exception/ApiErrorResponse.java
- [X] T007 [P] Create domain exceptions (ResourceNotFoundException, InsufficientPointsException, DuplicateEventException) in src/main/java/com/loyalty/shared/exception/
- [X] T008 Implement GlobalExceptionHandler with fail-fast validation and REST error handling in src/main/java/com/loyalty/shared/exception/GlobalExceptionHandler.java
- [X] T009 [P] Define Roles constants (ROLE_ADMIN, ROLE_CUSTOMER, ROLE_INTEGRATION_SERVICE) in src/main/java/com/loyalty/shared/security/Roles.java
- [X] T010 [P] Implement TenantSecurityContext holder for thread-safe tenant context in src/main/java/com/loyalty/shared/security/TenantSecurityContext.java
- [X] T011 Implement ApiKeyFilter for X-API-KEY validation and tenant resolution in src/main/java/com/loyalty/shared/security/ApiKeyFilter.java
- [X] T012 Implement JsonAuthenticationFilter for /api/auth/login BFF authentication in src/main/java/com/loyalty/shared/security/JsonAuthenticationFilter.java
- [X] T013 Configure three-tier Spring Security filter chains with 401 REST entry points in src/main/java/com/loyalty/shared/security/SecurityConfig.java
- [X] T014 Create V1 initial schema Flyway migration with tables, indices, and JSONB columns in src/main/resources/db/migration/V1__initial_schema.sql
- [X] T015 Create Tenant domain entity, TenantStatus enum, and repository in src/main/java/com/loyalty/tenant/domain/
- [X] T016 Implement TenantService for tenant lifecycle and lookup in src/main/java/com/loyalty/tenant/service/TenantService.java
- [ ] T017 Security integration test verifying 3 filter chains and 401 unauthenticated challenges in src/test/java/com/loyalty/security/MultiChainSecurityTest.java

**Checkpoint**: Foundation ready — user story implementation can now begin.

---

## Phase 3: User Story 1 - Multi-Tenant POS Integration & Customer Identity (Priority: P1) 🎯 MVP

**Goal**: Merchant onboarding, POS credentials provisioning (Kioskito adapter), and canonical customer profile resolution with external mapping and strict tenant isolation.

**Independent Test**: Can be tested by registering a tenant, provisioning POS integration credentials, posting a purchase event carrying an inline customer profile from Kioskito, and verifying that Customer and ExternalCustomerReference records are created with strict tenant isolation.

### Tests for User Story 1 ⚠️

- [ ] T018 [P] [US1] Integration test for tenant isolation and POS customer upsert in src/test/java/com/loyalty/customer/CustomerIdentityIntegrationTest.java

### Implementation for User Story 1

- [X] T019 [P] [US1] Create PosIntegration entity and EventStatus enum in src/main/java/com/loyalty/integration/domain/PosIntegration.java
- [X] T020 [P] [US1] Create PosIntegrationRepository in src/main/java/com/loyalty/integration/domain/PosIntegrationRepository.java
- [X] T021 [P] [US1] Create Customer and ExternalCustomerReference entities in src/main/java/com/loyalty/customer/domain/
- [X] T022 [P] [US1] Create CustomerRepository and ExternalCustomerReferenceRepository in src/main/java/com/loyalty/customer/domain/
- [X] T023 [P] [US1] Create CustomerResponseDTO and CustomerRequestDTO records with validation in src/main/java/com/loyalty/customer/api/dto/
- [X] T024 [US1] Implement PosIntegrationService for credential validation and tenant binding in src/main/java/com/loyalty/integration/service/PosIntegrationService.java
- [X] T025 [US1] Implement CustomerService with idempotent inline upsert and external reference mapping in src/main/java/com/loyalty/customer/service/CustomerService.java
- [X] T026 [US1] Implement CustomerController for customer profile queries in src/main/java/com/loyalty/customer/api/CustomerController.java

**Checkpoint**: User Story 1 is functional and testable independently. Canonical customer identity operates across tenants.

---

## Phase 4: User Story 2 - Idempotent Purchase Ingestion & Behavioral Statistics (Priority: P2)

**Goal**: Ingest POS events via `POST /api/v1/events` into an inbox, normalize purchase records, prevent duplicate processing, and maintain derived CustomerStats.

**Independent Test**: Can be tested by sending purchase, duplicate purchase, and cancellation/refund events to `/api/v1/events`; verifying HTTP 202 response within 500ms, exactly-once Purchase creation, and recalculated CustomerStats.

### Tests for User Story 2 ⚠️

- [ ] T027 [P] [US2] Contract test for POST /api/v1/events in src/test/java/com/loyalty/integration/EventIngestionContractTest.java
- [ ] T028 [P] [US2] Integration test for event deduplication and customer statistics in src/test/java/com/loyalty/integration/PosEventIngestionIntegrationTest.java

### Implementation for User Story 2

- [X] T029 [P] [US2] Create IngestEventRequestDTO and IngestEventResponseDTO records per contract in src/main/java/com/loyalty/integration/api/dto/
- [X] T030 [P] [US2] Create IntegrationEvent entity and IntegrationEventRepository with unique constraint in src/main/java/com/loyalty/integration/domain/
- [X] T031 [P] [US2] Create Purchase entity, PurchaseStatus enum, and PurchaseRepository in src/main/java/com/loyalty/purchase/domain/
- [X] T032 [P] [US2] Create CustomerStats entity and CustomerStatsRepository in src/main/java/com/loyalty/customer/domain/
- [X] T033 [P] [US2] Create CustomerStatsResponseDTO record in src/main/java/com/loyalty/customer/api/dto/CustomerStatsResponseDTO.java
- [X] T034 [US2] Implement PosNormalizationAdapter interface and KioskitoNormalizationAdapter in src/main/java/com/loyalty/integration/adapters/
- [X] T035 [US2] Implement CustomerStatsService for atomic statistical recalculation in src/main/java/com/loyalty/customer/service/CustomerStatsService.java
- [X] T036 [US2] Implement PurchaseService for recording purchases and handling cancellations/refunds in src/main/java/com/loyalty/purchase/service/PurchaseService.java
- [X] T037 [US2] Implement IntegrationEventInboxService for persistence, deduplication, and async processing worker in src/main/java/com/loyalty/integration/service/IntegrationEventInboxService.java
- [X] T038 [US2] Implement EventIngestionController exposing POST /api/v1/events in src/main/java/com/loyalty/integration/api/EventIngestionController.java

**Checkpoint**: User Stories 1 and 2 operate together. Incoming sales are reliably recorded, deduplicated, and analyzed in real-time.

---

## Phase 5: User Story 3 - Configurable Loyalty Program & Immutable Point Ledger (Priority: P3)

**Goal**: Configure loyalty programs with JSONB rules, manage customer loyalty accounts, credit points on purchases, handle redemptions, and maintain an immutable ledger with zero negative-balance guarantee.

**Independent Test**: Can be tested by configuring a points-per-spend program, processing a purchase to verify point accrual on the loyalty account, executing a point redemption, and verifying that ledger transactions are immutable and prevent negative balances.

### Tests for User Story 3 ⚠️

- [ ] T039 [P] [US3] Integration test for point accrual, redemptions, and compensatory reversals in src/test/java/com/loyalty/loyalty/LoyaltyLedgerIntegrationTest.java
- [ ] T040 [P] [US3] Contract test for customer loyalty balance and ledger endpoints in src/test/java/com/loyalty/loyalty/CustomerLoyaltyContractTest.java

### Implementation for User Story 3

- [X] T041 [P] [US3] Create LoyaltyProgram entity with JSONB configuration and LoyaltyProgramRepository in src/main/java/com/loyalty/loyalty/domain/
- [X] T042 [P] [US3] Create LoyaltyAccount entity with optimistic locking and points balance invariants in src/main/java/com/loyalty/loyalty/domain/LoyaltyAccount.java
- [X] T043 [P] [US3] Create PointTransaction entity, PointTransactionType enum, and PointTransactionRepository in src/main/java/com/loyalty/loyalty/domain/
- [X] T044 [P] [US3] Create CustomerLoyaltyResponseDTO and PointTransactionResponseDTO records in src/main/java/com/loyalty/loyalty/api/dto/
- [X] T045 [US3] Implement LoyaltyProgramService for program configuration management in src/main/java/com/loyalty/loyalty/service/LoyaltyProgramService.java
- [X] T046 [US3] Implement LoyaltyAccountService for atomic point accrual, idempotent redemption, and reversals in src/main/java/com/loyalty/loyalty/service/LoyaltyAccountService.java
- [X] T047 [US3] Implement LoyaltyController exposing GET /api/v1/customers/{customerId}/loyalty and /transactions in src/main/java/com/loyalty/loyalty/api/LoyaltyController.java
- [X] T048 [US3] Wire event processing pipeline to trigger LoyaltyAccount point accrual on purchase events in src/main/java/com/loyalty/integration/service/IntegrationEventInboxService.java

**Checkpoint**: Point ledger is fully functional, auditable, and mathematically consistent.

---

## Phase 6: User Story 4 - Reward Catalog & Single-Use Discount Coupons (Priority: P4)

**Goal**: Manage catalog rewards, exchange points for single-use coupons, validate coupons at POS, and redeem coupons idempotently.

**Independent Test**: Can be tested by creating a reward, exchanging points for a coupon code, validating coupon status via API, and redeeming the coupon once at checkout with double-spend prevention.

### Tests for User Story 4 ⚠️

- [ ] T049 [P] [US4] Contract test for coupon validation and redemption endpoints in src/test/java/com/loyalty/coupon/CouponContractTest.java
- [ ] T050 [P] [US4] Integration test for point-to-coupon exchange and POS coupon redemption in src/test/java/com/loyalty/coupon/CouponRedemptionIntegrationTest.java

### Implementation for User Story 4

- [X] T051 [P] [US4] Create Reward entity and RewardRepository in src/main/java/com/loyalty/reward/domain/
- [X] T052 [P] [US4] Create RewardRequestDTO and RewardResponseDTO records in src/main/java/com/loyalty/reward/api/dto/
- [X] T053 [P] [US4] Create Coupon entity, CouponStatus enum, DiscountType enum, and CouponRepository in src/main/java/com/loyalty/coupon/domain/
- [X] T054 [P] [US4] Create ValidateCouponRequestDTO, CouponValidationResponseDTO, RedeemCouponRequestDTO, RedeemCouponResponseDTO records in src/main/java/com/loyalty/coupon/api/dto/
- [X] T055 [US4] Implement RewardService for catalog management and points-to-reward redemption in src/main/java/com/loyalty/reward/service/RewardService.java
- [X] T056 [US4] Implement CouponService for coupon generation, validation, and single-use redemption in src/main/java/com/loyalty/coupon/service/CouponService.java
- [X] T057 [US4] Implement RewardController for reward catalog CRUD in src/main/java/com/loyalty/reward/api/RewardController.java
- [X] T058 [US4] Implement CouponController exposing POST /api/v1/coupons/validate and /redeem in src/main/java/com/loyalty/coupon/api/CouponController.java

**Checkpoint**: End-to-end reward and coupon loop is closed. Coupons can be validated and redeemed at POS.

---

## Phase 7: User Story 5 - Event-Driven Automations & Transactional Notifications (Priority: P5)

**Goal**: Automate customer re-engagement rules (e.g., CUSTOMER_INACTIVE after 30 days) to generate coupons and dispatch transactional emails with idempotency.

**Independent Test**: Can be tested by configuring an automation rule with CUSTOMER_INACTIVE trigger, triggering scheduled evaluation on an inactive customer profile, verifying AutomationExecution and AutomationAction records, and confirming coupon generation and email dispatch.

### Tests for User Story 5 ⚠️

- [ ] T059 [P] [US5] Integration test for inactive customer detection, coupon generation, and email dispatch in src/test/java/com/loyalty/automation/AutomationEngineIntegrationTest.java

### Implementation for User Story 5

- [X] T060 [P] [US5] Create Automation entity, TriggerType enum, and AutomationRepository in src/main/java/com/loyalty/automation/domain/
- [X] T061 [P] [US5] Create AutomationExecution and AutomationAction entities with ExecutionStatus and ActionType enums in src/main/java/com/loyalty/automation/domain/
- [X] T062 [P] [US5] Create AutomationExecutionRepository and AutomationActionRepository in src/main/java/com/loyalty/automation/domain/
- [X] T063 [P] [US5] Implement EmailNotificationService and EmailProviderAdapter interface in src/main/java/com/loyalty/notification/email/
- [X] T064 [P] [US5] Implement WhatsAppProviderAdapter interface stub in src/main/java/com/loyalty/notification/whatsapp/WhatsAppProviderAdapter.java
- [X] T065 [US5] Implement TriggerEvaluator interface and InactiveCustomerTriggerEvaluator in src/main/java/com/loyalty/automation/trigger/
- [X] T066 [US5] Implement ActionExecutor interface, GenerateCouponActionExecutor, and SendEmailActionExecutor in src/main/java/com/loyalty/automation/action/
- [X] T067 [US5] Implement AutomationEngineService with idempotency checks and scheduled evaluation job in src/main/java/com/loyalty/automation/service/AutomationEngineService.java

**Checkpoint**: Marketing automations react autonomously to customer lifecycle events.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: System documentation, OpenAPI/Swagger exposure, PDF generation, and full end-to-end quickstart validation.

- [X] T068 [P] Configure OpenAPI 3 documentation and Swagger UI in src/main/java/com/loyalty/shared/config/OpenApiConfig.java
- [X] T069 [P] Implement receipt/voucher PDF generation using OpenPDF in src/main/java/com/loyalty/shared/document/PdfReportGenerator.java
- [ ] T070 Execute and validate end-to-end quickstart scenarios from quickstart.md in src/test/java/com/loyalty/integration/QuickstartValidationEndToEndTest.java
- [X] T071 Verify zero-boilerplate checklist, record DTO immutability, and security 401 challenges across all modules

---

## Dependencies & Execution Order

### Phase Dependencies

```text
Phase 1: Setup
     │
     ▼
Phase 2: Foundational (Blocking Prerequisites)
     │
     ├──────────────────────┬──────────────────────┬──────────────────────┐
     ▼                      ▼                      ▼                      ▼
Phase 3: US1 (P1)     Phase 4: US2 (P2)     Phase 5: US3 (P3)     Phase 6: US4 (P4)
     │                      │                      │                      │
     └──────────────────────┼──────────────────────┴──────────────────────┘
                            ▼
                      Phase 7: US5 (P5)
                            │
                            ▼
                      Phase 8: Polish
```

- **Setup (Phase 1)**: Can start immediately.
- **Foundational (Phase 2)**: Depends on Setup; BLOCKS all user stories.
- **User Story 1 (P1)**: Can start as soon as Phase 2 completes.
- **User Story 2 (P2)**: Can start after Phase 2 completes; utilizes Customer identity from US1.
- **User Story 3 (P3)**: Can start after Phase 2 completes; integrates with Purchase ingestion from US2.
- **User Story 4 (P4)**: Can start after Phase 2 completes; integrates with Loyalty points from US3.
- **User Story 5 (P5)**: Depends on CustomerStats (US2), Coupons (US4), and event infrastructure.
- **Polish (Phase 8)**: Depends on completion of user stories.

### Parallel Execution Opportunities

- In Phase 1: T003 and T004 can run in parallel.
- In Phase 2: T005, T006, T007, T009, T010 can run in parallel.
- In Phase 3 (US1): T018 (Test), T019, T020, T021, T022, T023 can run in parallel.
- In Phase 4 (US2): T027, T028 (Tests), T029, T030, T031, T032, T033 can run in parallel.
- In Phase 5 (US3): T039, T040 (Tests), T041, T042, T043, T044 can run in parallel.
- In Phase 6 (US4): T049, T050 (Tests), T051, T052, T053, T054 can run in parallel.
- In Phase 7 (US5): T059 (Test), T060, T061, T062, T063, T064 can run in parallel.
- In Phase 8: T068, T069 can run in parallel.

---

## Parallel Example: User Story 1 (US1)

```bash
# Launch test and DTO/entity creation for US1 in parallel:
Task: T018 [P] [US1] Integration test for tenant isolation in src/test/java/com/loyalty/customer/CustomerIdentityIntegrationTest.java
Task: T019 [P] [US1] Create PosIntegration entity in src/main/java/com/loyalty/integration/domain/PosIntegration.java
Task: T021 [P] [US1] Create Customer entity in src/main/java/com/loyalty/customer/domain/Customer.java
Task: T023 [P] [US1] Create CustomerResponseDTO record in src/main/java/com/loyalty/customer/api/dto/CustomerResponseDTO.java
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001 - T004).
2. Complete Phase 2: Foundational (T005 - T017).
3. Complete Phase 3: User Story 1 (T018 - T026).
4. **STOP and VALIDATE**: Verify CustomerIdentityIntegrationTest and multi-tenant isolation.
5. Commit each task atomically using Conventional Commits.

### Incremental Delivery

1. Phase 1 & 2: Architectural foundation & 3-tier security in place.
2. Deliver US1 (P1): Merchant POS connection & customer profile normalization.
3. Deliver US2 (P2): POS event ingestion, deduplication inbox, and customer behavioral statistics.
4. Deliver US3 (P3): Point ledger rules and balance calculations.
5. Deliver US4 (P4): Reward redemption and single-use discount coupons at POS.
6. Deliver US5 (P5): Autonomous re-engagement engine and transactional emails.
7. Phase 8: Cross-cutting polish, OpenAPI documentation, and end-to-end quickstart validation.
