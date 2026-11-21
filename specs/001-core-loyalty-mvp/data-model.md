# Data Model: Loyalty Engine Core MVP

**Feature**: `001-core-loyalty-mvp`  
**Date**: 2026-11-21  
**Status**: Completed

## 1. Relational Entity Diagram

```text
  ┌──────────────┐ 1      * ┌────────────────┐
  │    Tenant    ├──────────┤ PosIntegration │
  └──────┬───────┘          └────────────────┘
         │ 1
         ├──────────────────────┐
         │ *                    │ *
  ┌──────▼───────┐       ┌──────▼────────┐
  │   Customer   │       │ LoyaltyProgram│
  └──────┬───────┘       └──────┬────────┘
         │ 1                    │ 1
         │                      │
         │ *                    │ *
  ┌──────▼───────┐       ┌──────▼────────┐ 1      * ┌──────────────────┐
  │   Purchase   │       │ LoyaltyAccount├──────────┤ PointTransaction │
  └──────────────┘       └──────┬────────┘          └──────────────────┘
                                │ 1
                                │ *
                         ┌──────▼────────┐
                         │    Coupon     │
                         └───────────────┘
```

---

## 2. Entity Specifications

### 2.1 Tenant (`tenants`)
Represents an isolated merchant organization on the platform.
- `id` (`UUID`, PK): Unique tenant identifier.
- `name` (`VARCHAR(128)`, NOT NULL): Business/brand name.
- `status` (`VARCHAR(32)`, NOT NULL): `ACTIVE`, `SUSPENDED`.
- `created_at` (`TIMESTAMPTZ`, NOT NULL): Creation timestamp.

### 2.2 PosIntegration (`pos_integrations`)
Connection configuration and credentials for an external POS system.
- `id` (`UUID`, PK): Unique integration identifier.
- `tenant_id` (`UUID`, FK -> `tenants.id`, NOT NULL): Owning tenant.
- `provider` (`VARCHAR(64)`, NOT NULL): e.g., `"kioskito"`.
- `external_store_id` (`VARCHAR(128)`, NOT NULL): Store ID in the external POS.
- `api_key_hash` (`VARCHAR(256)`, NOT NULL): SHA-256 hash of API Key used for M2M auth.
- `encrypted_credentials` (`TEXT`, NOT NULL): Encrypted API tokens or secrets for outbound callbacks.
- `status` (`VARCHAR(32)`, NOT NULL): `ACTIVE`, `DISCONNECTED`, `ERROR`.
- `created_at` (`TIMESTAMPTZ`, NOT NULL).
- **Constraints**: `UNIQUE(provider, external_store_id)`.

### 2.3 Customer (`customers`)
Canonical internal customer identity.
- `id` (`UUID`, PK): System-generated internal customer UUID.
- `tenant_id` (`UUID`, FK -> `tenants.id`, NOT NULL): Scoped merchant.
- `name` (`VARCHAR(128)`, NOT NULL): Customer full name.
- `email` (`VARCHAR(256)`, NULLABLE): Normalized email address.
- `phone` (`VARCHAR(64)`, NULLABLE): E.164 formatted phone number.
- `birth_date` (`DATE`, NULLABLE): Date of birth for birthday triggers.
- `created_at` (`TIMESTAMPTZ`, NOT NULL).
- `updated_at` (`TIMESTAMPTZ`, NOT NULL).
- **Constraints**: `UNIQUE(tenant_id, email)` (where email is not null).

### 2.4 ExternalCustomerReference (`external_customer_references`)
Mapping between internal customer and POS-specific customer identifiers.
- `id` (`UUID`, PK).
- `customer_id` (`UUID`, FK -> `customers.id`, NOT NULL).
- `integration_id` (`UUID`, FK -> `pos_integrations.id`, NOT NULL).
- `external_id` (`VARCHAR(128)`, NOT NULL): Identifier in the POS system.
- `created_at` (`TIMESTAMPTZ`, NOT NULL).
- **Constraints**: `UNIQUE(integration_id, external_id)`.

### 2.5 Purchase (`purchases`)
Normalized sales record.
- `id` (`UUID`, PK).
- `tenant_id` (`UUID`, FK -> `tenants.id`, NOT NULL).
- `customer_id` (`UUID`, FK -> `customers.id`, NOT NULL).
- `integration_id` (`UUID`, FK -> `pos_integrations.id`, NOT NULL).
- `external_id` (`VARCHAR(128)`, NOT NULL): Ticket/invoice ID in POS.
- `total` (`NUMERIC(14,2)`, NOT NULL): Total transaction amount.
- `currency` (`VARCHAR(3)`, NOT NULL): ISO currency code (e.g., `"ARS"`, `"USD"`).
- `status` (`VARCHAR(32)`, NOT NULL): `COMPLETED`, `PARTIALLY_REFUNDED`, `REFUNDED`, `CANCELLED`.
- `purchased_at` (`TIMESTAMPTZ`, NOT NULL): Time purchase occurred at POS.
- `created_at` (`TIMESTAMPTZ`, NOT NULL).
- **Constraints**: `UNIQUE(integration_id, external_id)`.

### 2.6 CustomerStats (`customer_stats`)
Materialized analytical summary projection for customer behavior.
- `customer_id` (`UUID`, PK, FK -> `customers.id`).
- `tenant_id` (`UUID`, FK -> `tenants.id`, NOT NULL).
- `total_purchases` (`INTEGER`, NOT NULL, DEFAULT 0).
- `total_spent` (`NUMERIC(14,2)`, NOT NULL, DEFAULT 0.00).
- `average_ticket` (`NUMERIC(14,2)`, NOT NULL, DEFAULT 0.00).
- `first_purchase_at` (`TIMESTAMPTZ`, NULLABLE).
- `last_purchase_at` (`TIMESTAMPTZ`, NULLABLE).
- `updated_at` (`TIMESTAMPTZ`, NOT NULL).

### 2.7 LoyaltyProgram (`loyalty_programs`)
Merchant-configured loyalty program definitions.
- `id` (`UUID`, PK).
- `tenant_id` (`UUID`, FK -> `tenants.id`, NOT NULL).
- `name` (`VARCHAR(128)`, NOT NULL): e.g., "Puntos Kioskito".
- `type` (`VARCHAR(32)`, NOT NULL): `POINTS`, `COUPONS`, `TIERS`.
- `configuration` (`JSONB`, NOT NULL): JSON rules, e.g. `{"pointsPerSpent": 100, "expirationMonths": 12}`.
- `enabled` (`BOOLEAN`, NOT NULL, DEFAULT true).
- `created_at` (`TIMESTAMPTZ`, NOT NULL).

### 2.8 LoyaltyAccount (`loyalty_accounts`)
Customer's active loyalty account for a specific program.
- `id` (`UUID`, PK).
- `tenant_id` (`UUID`, FK -> `tenants.id`, NOT NULL).
- `customer_id` (`UUID`, FK -> `customers.id`, NOT NULL).
- `program_id` (`UUID`, FK -> `loyalty_programs.id`, NOT NULL).
- `points_balance` (`BIGINT`, NOT NULL, DEFAULT 0).
- `lifetime_points` (`BIGINT`, NOT NULL, DEFAULT 0).
- `version` (`BIGINT`, NOT NULL, DEFAULT 0): Optimistic locking version.
- `created_at` (`TIMESTAMPTZ`, NOT NULL).
- `updated_at` (`TIMESTAMPTZ`, NOT NULL).
- **Constraints**: `UNIQUE(program_id, customer_id)`, CHECK (`points_balance >= 0`).

### 2.9 PointTransaction (`point_transactions`)
Immutable ledger record of all point movements.
- `id` (`UUID`, PK).
- `account_id` (`UUID`, FK -> `loyalty_accounts.id`, NOT NULL).
- `type` (`VARCHAR(32)`, NOT NULL): `EARN`, `REDEEM`, `EXPIRE`, `ADJUST`, `REVERSAL`.
- `amount` (`BIGINT`, NOT NULL): Positive for credits, negative for debits.
- `reference_id` (`UUID`, NULLABLE): Purchase ID, Reward ID, or reversed transaction ID.
- `idempotency_key` (`VARCHAR(128)`, NOT NULL).
- `description` (`VARCHAR(255)`, NOT NULL).
- `created_at` (`TIMESTAMPTZ`, NOT NULL).
- **Constraints**: `UNIQUE(account_id, idempotency_key)`.

### 2.10 Reward (`rewards`)
Catalog perk or voucher.
- `id` (`UUID`, PK).
- `tenant_id` (`UUID`, FK -> `tenants.id`, NOT NULL).
- `name` (`VARCHAR(128)`, NOT NULL).
- `description` (`TEXT`, NOT NULL).
- `points_cost` (`BIGINT`, NOT NULL).
- `active` (`BOOLEAN`, NOT NULL, DEFAULT true).
- `created_at` (`TIMESTAMPTZ`, NOT NULL).

### 2.11 Coupon (`coupons`)
Unique, redeemable coupon code assigned to a customer.
- `id` (`UUID`, PK).
- `tenant_id` (`UUID`, FK -> `tenants.id`, NOT NULL).
- `customer_id` (`UUID`, FK -> `customers.id`, NOT NULL).
- `reward_id` (`UUID`, FK -> `rewards.id`, NULLABLE).
- `code` (`VARCHAR(64)`, NOT NULL): Unique alphanumeric coupon code.
- `discount_type` (`VARCHAR(32)`, NOT NULL): `PERCENTAGE`, `FIXED_AMOUNT`.
- `discount_value` (`NUMERIC(14,2)`, NOT NULL).
- `status` (`VARCHAR(32)`, NOT NULL): `AVAILABLE`, `REDEEMED`, `EXPIRED`, `CANCELLED`.
- `expires_at` (`TIMESTAMPTZ`, NULLABLE).
- `redeemed_at` (`TIMESTAMPTZ`, NULLABLE).
- `created_at` (`TIMESTAMPTZ`, NOT NULL).
- **Constraints**: `UNIQUE(tenant_id, code)`.

### 2.12 Automation (`automations`)
Predefined marketing or loyalty rule definition.
- `id` (`UUID`, PK).
- `tenant_id` (`UUID`, FK -> `tenants.id`, NOT NULL).
- `name` (`VARCHAR(128)`, NOT NULL).
- `trigger_type` (`VARCHAR(64)`, NOT NULL): `CUSTOMER_INACTIVE`, `FIRST_PURCHASE`, `BIRTHDAY`, etc.
- `configuration` (`JSONB`, NOT NULL): Parameterized trigger, conditions, and action definitions.
- `enabled` (`BOOLEAN`, NOT NULL, DEFAULT true).
- `created_at` (`TIMESTAMPTZ`, NOT NULL).

### 2.13 AutomationExecution (`automation_executions`)
Audit record of a triggered automation.
- `id` (`UUID`, PK).
- `automation_id` (`UUID`, FK -> `automations.id`, NOT NULL).
- `customer_id` (`UUID`, FK -> `customers.id`, NOT NULL).
- `trigger_event_id` (`UUID`, NULLABLE).
- `idempotency_key` (`VARCHAR(128)`, NOT NULL).
- `status` (`VARCHAR(32)`, NOT NULL): `PENDING`, `RUNNING`, `COMPLETED`, `FAILED`.
- `triggered_at` (`TIMESTAMPTZ`, NOT NULL).
- `completed_at` (`TIMESTAMPTZ`, NULLABLE).
- `error_message` (`TEXT`, NULLABLE).
- **Constraints**: `UNIQUE(automation_id, idempotency_key)`.

### 2.14 AutomationAction (`automation_actions`)
Individual action executed within an automation execution.
- `id` (`UUID`, PK).
- `execution_id` (`UUID`, FK -> `automation_executions.id`, NOT NULL).
- `action_type` (`VARCHAR(64)`, NOT NULL): `GENERATE_COUPON`, `SEND_EMAIL`, `ADD_POINTS`.
- `status` (`VARCHAR(32)`, NOT NULL): `SUCCESS`, `FAILED`.
- `result` (`JSONB`, NOT NULL).
- `executed_at` (`TIMESTAMPTZ`, NOT NULL).

### 2.15 IntegrationEvent (`integration_events`)
Raw inbox for events arriving from external POS systems.
- `id` (`UUID`, PK).
- `tenant_id` (`UUID`, FK -> `tenants.id`, NOT NULL).
- `integration_id` (`UUID`, FK -> `pos_integrations.id`, NOT NULL).
- `external_event_id` (`VARCHAR(128)`, NOT NULL).
- `event_type` (`VARCHAR(64)`, NOT NULL): `PURCHASE_COMPLETED`, `CUSTOMER_UPDATED`, etc.
- `payload` (`JSONB`, NOT NULL).
- `status` (`VARCHAR(32)`, NOT NULL): `PENDING`, `PROCESSING`, `PROCESSED`, `FAILED`.
- `received_at` (`TIMESTAMPTZ`, NOT NULL).
- `processed_at` (`TIMESTAMPTZ`, NULLABLE).
- **Constraints**: `UNIQUE(integration_id, external_event_id)`.
- **Note**: `POST /api/v1/events` is the only MVP ingestion path. The optional `data.customer` object carries profile upserts; there is no separate customer-sync endpoint.
