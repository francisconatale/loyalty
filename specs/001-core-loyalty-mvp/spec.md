# Feature Specification: Loyalty Engine Core MVP

**Feature Branch**: `001-core-loyalty-mvp`

**Created**: 2026-11-21

**Status**: Draft

**Input**: User description: "Loyalty Engine — Especificación funcional, modelo de dominio y arquitectura v0.1"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Multi-Tenant POS Integration & Customer Identity (Priority: P1)

As a merchant using a POS system (e.g., Kioskito), I want to connect my store to Loyalty Engine and keep customer profiles up to date automatically as purchase events arrive, so that my business can build a centralized customer identity regardless of external POS changes.

**Why this priority**: Merchant onboarding, POS integration credentials, and canonical customer profile resolution are foundational prerequisites for every downstream loyalty calculation and transaction ledger.

**Independent Test**: Can be fully tested by registering a tenant, provisioning POS credentials, posting a first purchase event that carries an optional customer profile from Kioskito, and verifying that the canonical customer profile and external mapping are created with proper tenant isolation.

**Acceptance Scenarios**:

1. **Given** a registered and active tenant, **When** POS integration credentials are created for provider "Kioskito", **Then** the integration record is stored with encrypted credentials and active status.
2. **Given** an active POS integration, **When** a purchase event with a new external customer ID `cust-001` and an inline customer profile is received, **Then** an internal customer entity is created, linked to the tenant, and mapped via an external reference with unique constraint `(integration_id, external_id)`.
3. **Given** an existing customer with external reference `cust-001`, **When** a later purchase event carries modified contact data, **Then** the existing customer record is updated without creating duplicate profiles.
4. **Given** two distinct tenants, **When** both register customers with the same external identifier, **Then** each tenant maintains isolated records without data leakage or collision.

---

### User Story 2 - Idempotent Purchase Ingestion & Behavioral Statistics (Priority: P2)

As a merchant, I want incoming purchase events, cancellations, and refunds from my POS to be recorded reliably without duplicates so that customer purchase history and behavioral metrics are accurate and tamper-proof.

**Why this priority**: Loyalty points and automation triggers directly depend on valid, deduplicated purchase records and up-to-date customer behavior statistics (total spent, average ticket, purchase frequency).

**Independent Test**: Can be tested by sending purchase, cancellation, and refund events to the integration event inbox, verifying idempotent processing, state transitions on the purchase record, and updated customer statistics.

**Acceptance Scenarios**:

1. **Given** an active integration, **When** a purchase event with external purchase ID `sale-100` and total `$25,000 ARS` is received at the event ingestion endpoint, **Then** the raw event is persisted in the event inbox, normalized into a `Purchase` entity, and customer statistics (`totalPurchases`, `totalSpent`, `averageTicket`, `lastPurchaseAt`) are recomputed.
2. **Given** a purchase event already processed, **When** a duplicate event with identical `(integration_id, external_id)` arrives, **Then** the system acknowledges receipt without creating duplicate purchases or double-counting statistics.
3. **Given** an existing purchase, **When** a refund or cancellation event is received, **Then** the purchase status transitions to `REFUNDED` or `CANCELLED` and customer statistics adjust accordingly.

---

### User Story 3 - Configurable Loyalty Program & Immutable Point Ledger (Priority: P3)

As a merchant, I want to define point accrual rules and manage customer loyalty accounts so that customers earn points on completed purchases and can redeem them for rewards while guaranteeing ledger auditability.

**Why this priority**: The loyalty program is the core value proposition of the platform. Accurate point balances and non-repudiable ledgers are essential for financial trust between merchant and customer.

**Independent Test**: Can be tested by configuring a points-per-spend program, processing a purchase to verify point accrual on the loyalty account, executing a point redemption, and verifying that ledger transactions are immutable and prevent negative balances.

**Acceptance Scenarios**:

1. **Given** an active loyalty program configured with rule "1 point per $100 spent", **When** a purchase of `$25,000` is processed, **Then** 250 points are credited to the customer's `LoyaltyAccount` and an immutable `PointTransaction` of type `EARN` is appended to the ledger.
2. **Given** an account with 250 points, **When** a redemption request for 200 points is submitted with an idempotency key, **Then** 200 points are deducted, a `REDEEM` ledger entry is appended, and the new balance is 50.
3. **Given** an account with 50 points, **When** a redemption request for 100 points is submitted, **Then** the system rejects the transaction with an insufficient balance domain error and balance remains 50.
4. **Given** a purchase that accrued 250 points, **When** the purchase is cancelled or refunded, **Then** a compensatory `PointTransaction` of type `REVERSAL` is recorded, decreasing the balance without mutating the original `EARN` transaction.

---

### User Story 4 - Reward Catalog & Single-Use Discount Coupons (Priority: P4)

As a merchant and customer, I want available rewards to be converted into redeemable coupons that can be validated at the POS so that customers receive tangible benefits from their loyalty points.

**Why this priority**: Closes the loop on customer satisfaction by allowing accumulated points to be spent on real-world discounts and perks.

**Independent Test**: Can be tested by creating a reward, exchanging points for a coupon code, validating coupon status via API, and redeeming the coupon once at checkout.

**Acceptance Scenarios**:

1. **Given** an active reward "15% Off Total Ticket" costing 200 points, **When** a customer with sufficient points redeems it, **Then** 200 points are deducted from their account and an active `Coupon` with a unique code and expiration timestamp is issued.
2. **Given** an active coupon code, **When** the POS queries coupon validation at `/api/v1/coupons/validate`, **Then** the system returns validity, discount type, and discount value.
3. **Given** an active coupon, **When** the POS confirms redemption at `/api/v1/coupons/redeem`, **Then** the coupon status transitions to `REDEEMED`, recording the redemption timestamp.
4. **Given** an already redeemed or expired coupon, **When** POS attempts validation or redemption, **Then** the system rejects the request with an explicit error status.

---

### User Story 5 - Event-Driven Automations & Transactional Notifications (Priority: P5)

As a merchant, I want automated marketing rules that trigger customer re-engagement actions (such as generating a coupon and sending an email when a customer is inactive for 30 days) so that customer retention increases autonomously.

**Why this priority**: Automations turn static loyalty data into an active revenue engine for merchants by proactively engaging customers at high-impact moments.

**Independent Test**: Can be tested by configuring an automation with trigger `CUSTOMER_INACTIVE` (30 days), evaluating a qualified customer profile, verifying execution records for idempotency, generating a retention coupon, and emitting the transactional email notification.

**Acceptance Scenarios**:

1. **Given** an active automation rule with trigger `CUSTOMER_INACTIVE` (days: 30), condition `HAS_EMAIL == true`, and actions `GENERATE_COUPON` and `SEND_EMAIL`, **When** the scheduler detects a customer with `lastPurchaseAt` > 30 days ago, **Then** an `AutomationExecution` is created with an idempotency key.
2. **Given** an automation execution, **When** the action executor processes `GENERATE_COUPON`, **Then** a single-use coupon is created for the customer and its details are recorded in `AutomationAction`.
3. **Given** the generated coupon, **When** the action executor processes `SEND_EMAIL`, **Then** the email notification payload is dispatched to the transactional email provider adapter and marked completed.
4. **Given** an already executed automation for a customer in the current eligibility window, **When** the evaluation job runs again, **Then** the idempotency check prevents duplicate coupon generation or email spam.

---

### Edge Cases

- **Out-of-order POS events**: What happens when a purchase cancellation or refund arrives before the original purchase event? The system queues the event in the inbox and rejects state transition until the parent purchase is resolved, or records a pending reversal.
- **Concurrent redemptions**: What happens when two redemption requests arrive simultaneously for the same account with only enough balance for one? Optimistic locking or database-level row locking on `LoyaltyAccount` ensures only one transaction succeeds; the second fails with insufficient balance.
- **Refund exceeding current balance**: What happens when a customer spends points and subsequently returns the original purchase that awarded those points? The reversal ledger entry records the point deduction, bringing points to the permitted floor (zero balance floor policy) while flagging the account for review.
- **Transient notification adapter failure**: What happens if the external email provider times out? The `AutomationAction` is marked `FAILED` with retry metadata, allowing the worker to retry without regenerating duplicate coupons.
- **Customer with missing email or phone**: What happens when an automation requires email communication but the POS profile lacked contact details? The automation condition check fails gracefully, skipping the notification action and logging the outcome in the tracking ledger.

## Requirements *(mandatory)*

### Functional Requirements

#### Multi-Tenancy & Security Boundaries
- **FR-001**: System MUST strictly isolate all data, queries, and operations by `tenantId`.
- **FR-002**: System MUST authenticate POS integrations via secure API Key or credentials, mapping authenticated requests directly to the owning tenant context.
- **FR-003**: System MUST NOT accept unverified or caller-supplied tenant identifiers in request payloads.
- **FR-004**: System MUST store all third-party integration credentials in encrypted format.

#### Integration Ingestion & Event Inbox
- **FR-005**: System MUST ingest POS events through a single REST endpoint (`POST /api/v1/events`), with an optional inline customer profile in the event payload; separate webhook and customer-sync endpoints are not part of the MVP.
- **FR-006**: System MUST persist incoming raw integration events into an `IntegrationEvent` inbox before processing, assigning an idempotency key and tracking status (`PENDING`, `PROCESSING`, `PROCESSED`, `FAILED`).
- **FR-007**: System MUST acknowledge event delivery within 500ms of persistence in the inbox.
- **FR-008**: System MUST deduplicate incoming events based on `(integration_id, external_id)` to guarantee exactly-once business processing.

#### Customer Identity & Canonical Model
- **FR-009**: System MUST maintain canonical internal `Customer` profiles referenced by system-generated UUIDs.
- **FR-010**: System MUST link external POS identifiers to internal customer records via `ExternalCustomerReference` with a unique constraint on `(integration_id, external_id)`.
- **FR-011**: System MUST update customer profiles upon receiving updated contact information from external POS feeds without duplicating customer records.

#### Purchases & Behavioral Analytics
- **FR-012**: System MUST record normalized `Purchase` entities with unique constraint on `(integration_id, external_id)`.
- **FR-013**: System MUST track purchase lifecycles through explicit statuses: `COMPLETED`, `PARTIALLY_REFUNDED`, `REFUNDED`, `CANCELLED`.
- **FR-014**: System MUST maintain derived `CustomerStats` (total purchases, total spent, average ticket, first purchase date, last purchase date) recalculating them upon purchase state changes.

#### Loyalty Accounts & Immutable Point Ledger
- **FR-015**: System MUST support configurable `LoyaltyProgram` entities per tenant with dynamic JSONB rule parameters (points-per-spend, expiration, multipliers).
- **FR-016**: System MUST maintain exactly one `LoyaltyAccount` per `(program_id, customer_id)`.
- **FR-017**: System MUST record all point balance changes as immutable `PointTransaction` ledger entries (`EARN`, `REDEEM`, `EXPIRE`, `ADJUST`, `REVERSAL`).
- **FR-018**: System MUST forbid any transaction or redemption that would result in a negative point balance.
- **FR-019**: System MUST issue compensatory `REVERSAL` ledger transactions when purchases are cancelled or refunded, never altering historical entries.

#### Rewards & Coupon Management
- **FR-020**: System MUST manage a tenant-scoped `Reward` catalog specifying point costs and active availability.
- **FR-021**: System MUST issue single-use `Coupon` records upon reward redemption or automation triggers with unique coupon codes and optional expiration dates.
- **FR-022**: System MUST provide POS endpoints to validate coupon status (`AVAILABLE`, `REDEEMED`, `EXPIRED`, `CANCELLED`) and redeem coupons idempotently.

#### Automation Engine & Notifications
- **FR-023**: System MUST support predefined, parameter-validated triggers: `FIRST_PURCHASE`, `SECOND_PURCHASE`, `PURCHASE_COMPLETED`, `CUSTOMER_INACTIVE`, `BIRTHDAY`, `PURCHASE_MILESTONE`, `SPENDING_THRESHOLD`, `POINTS_THRESHOLD`.
- **FR-024**: System MUST evaluate declarative conditions (purchase count, cumulative spending, days inactive, points balance, contact channel availability) without executing arbitrary unvetted code.
- **FR-025**: System MUST execute predefined actions (`ADD_POINTS`, `GENERATE_COUPON`, `SEND_EMAIL`, `SEND_WHATSAPP`, `ASSIGN_REWARD`).
- **FR-026**: System MUST record each automation execution in `AutomationExecution` and individual actions in `AutomationAction` with idempotency keys to prevent repeated executions.
- **FR-027**: System MUST dispatch transactional notifications via dedicated adapter interfaces (Email adapter for MVP; WhatsApp adapter interface defined for subsequent rollout).

### Key Entities

- **Tenant**: Represents an independent merchant using the platform (`id`, `name`, `status`, `createdAt`).
- **PosIntegration**: Connection settings and credentials for a specific POS system (`id`, `tenantId`, `provider`, `externalStoreId`, `status`, `credentials`, `createdAt`).
- **Customer**: Canonical customer identity (`id`, `tenantId`, `name`, `email`, `phone`, `birthDate`, `createdAt`, `updatedAt`).
- **ExternalCustomerReference**: Mapping between internal customer and external POS IDs (`id`, `customerId`, `integrationId`, `externalId`).
- **Purchase**: Normalized sales transaction (`id`, `tenantId`, `customerId`, `integrationId`, `externalId`, `total`, `currency`, `status`, `purchasedAt`, `createdAt`).
- **CustomerStats**: Derived analytical projection (`customerId`, `totalPurchases`, `totalSpent`, `averageTicket`, `firstPurchaseAt`, `lastPurchaseAt`).
- **LoyaltyProgram**: Loyalty configuration defined by merchant (`id`, `tenantId`, `name`, `type`, `configuration`, `enabled`, `createdAt`).
- **LoyaltyAccount**: Customer balance holder for a program (`id`, `tenantId`, `customerId`, `programId`, `pointsBalance`, `lifetimePoints`, `createdAt`).
- **PointTransaction**: Immutable point ledger record (`id`, `accountId`, `type`, `amount`, `referenceId`, `idempotencyKey`, `description`, `createdAt`).
- **Reward**: Catalog perk or voucher (`id`, `tenantId`, `name`, `description`, `pointsCost`, `active`, `createdAt`).
- **Coupon**: Redeemable discount coupon assigned to a customer (`id`, `tenantId`, `customerId`, `rewardId`, `code`, `discountType`, `discountValue`, `status`, `expiresAt`, `redeemedAt`).
- **Automation**: Predefined business rule definition (`id`, `tenantId`, `name`, `triggerType`, `configuration`, `enabled`, `createdAt`).
- **AutomationExecution**: Audit record of a rule invocation (`id`, `automationId`, `customerId`, `triggerEventId`, `idempotencyKey`, `status`, `triggeredAt`, `completedAt`, `errorMessage`).
- **AutomationAction**: Single action executed as part of an automation (`id`, `executionId`, `actionType`, `status`, `result`, `executedAt`).
- **IntegrationEvent**: Raw event inbox message (`id`, `tenantId`, `integrationId`, `externalEventId`, `eventType`, `payload`, `status`, `receivedAt`, `processedAt`).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: POS event ingestion acknowledges event receipt and persistence in the event inbox within 500 milliseconds (p95).
- **SC-002**: 100% of duplicate POS event submissions with identical integration and external event IDs are deduplicated without double processing.
- **SC-003**: End-to-end processing from inbox ingestion to point ledger crediting completes within 5 seconds for normal load.
- **SC-004**: Point balance calculations maintain 100% mathematical integrity against the cumulative sum of immutable ledger transactions across all accounts.
- **SC-005**: 100% of coupon redemptions at POS are strictly single-use; subsequent validation or redemption attempts on a used coupon are rejected.
- **SC-006**: Inactive customer re-engagement automations execute and dispatch single-use recovery coupons and emails within 15 minutes of scheduled trigger detection.

## Assumptions

- **Single POS MVP**: The initial deliverable targets Kioskito POS adapter as the canonical validation case; multi-provider concurrent deployments are planned for subsequent phases.
- **Single Ingestion Endpoint**: The MVP exposes only `POST /api/v1/events`. Webhook callbacks and separate customer-sync endpoints are deferred post-MVP; customer profile updates travel inside the purchase event.
- **Email First, WhatsApp Ready**: Transactional email notifications will be implemented via external provider adapter for the MVP. WhatsApp notifications share the same notification contract but adapter implementation is scheduled post-MVP.
- **Modular Monolith**: Deployment is architected as a modular Spring Boot monolith with clean boundary packages (`tenant`, `integration`, `customer`, `purchase`, `analytics`, `loyalty`, `reward`, `coupon`, `automation`, `notification`, `tracking`), deferring distributed message brokers or microservices.
- **Event Inbox Polling/Worker**: Background processing of `IntegrationEvent` records is handled via scheduled application workers and Spring Task execution with idempotency guarantees.
- **Currency & Decimal Precision**: Monetary amounts use `BigDecimal` with 2-decimal standard representation, and point amounts are recorded as integer `Long` values.
