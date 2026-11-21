# Research: Loyalty Engine Core MVP Architecture & Patterns

**Feature**: `001-core-loyalty-mvp`  
**Date**: 2026-11-21  
**Status**: Completed

## 1. Technical Choices & Framework Decisions

### 1.1 Java 17 & Spring Boot 3.2.5
- **Decision**: Standardize on Java 17 and Spring Boot 3.2.5.
- **Rationale**: Meets zero-boilerplate constitutional principles natively through Java `record` for all REST request/response contracts, pattern matching, sealed interfaces, and native Jakarta EE 10 / Hibernate 6 integration.
- **Alternatives Considered**: Java 21 was considered, but `pom.xml` explicitly locks Java 17 and Spring Boot 3.2.5 with existing starter dependencies.

### 1.2 Persistence & JSONB Strategy
- **Decision**: PostgreSQL with Hibernate 6 `@JdbcTypeCode(SqlTypes.JSON)` for semi-structured fields (`configuration` in `LoyaltyProgram`, `configuration` in `Automation`, `result` in `AutomationAction`, and `payload` in `IntegrationEvent`).
- **Rationale**: Eliminates fragile EAV (Entity-Attribute-Value) tables while preserving PostgreSQL JSONB indexing and query operators (`@>`, `->`, `->>`). Retains strict relational foreign keys for canonical entities (`Tenant`, `Customer`, `Purchase`, `LoyaltyAccount`, `PointTransaction`).
- **Alternatives Considered**: Generic string columns or dedicated JSON conversion classes with custom Hibernate UserTypes; rejected because Hibernate 6 provides first-class standard support via `@JdbcTypeCode(SqlTypes.JSON)`.

### 1.3 Asynchronous Event Processing & Ingestion Inbox
- **Decision**: Persist raw event payloads into an `IntegrationEvent` table (`status = PENDING`) with an immediate `HTTP 202 Accepted` response. A scheduled application worker (`@Scheduled` / Spring Task Executor with optimistic locking `SKIP LOCKED` or batch chunking) deserializes and dispatches to canonical domain services. Customer profiles travel inline in `data.customer`, so no separate sync endpoint is needed.
- **Rationale**:
  1. Guarantees event ingestion response times under 500ms even during peak store activity.
  2. Guarantees idempotency via `UNIQUE(integration_id, external_event_id)`.
  3. Provides complete auditability and replayability without introducing Kafka, RabbitMQ, or external brokers (conforming to the modular monolith constraint).
- **Alternatives Considered**:
  - In-memory processing directly inside controller thread: Rejected due to risk of event timeouts and unhandled failure drops.
  - External Kafka/RabbitMQ broker: Explicitly ruled out in MVP specification section 2.2.

### 1.4 POS Normalization Adapter Pattern (Kioskito)
- **Decision**: Ports and Adapters (Hexagonal) pattern for external POS providers. The `integration` module defines the canonical port interface `PosNormalizationAdapter`. Provider packages (e.g., `adapters.kioskito`) translate Kioskito-specific customer and purchase payloads into internal command records (`UpsertCustomerCommand`, `RecordPurchaseCommand`).
- **Rationale**: Shields core domains (`Customer`, `Purchase`, `Loyalty`) from external schema changes, vendor naming quirks, or currency representations.
- **Alternatives Considered**: Having controllers parse provider-specific formats directly into domain entities; rejected because it violates Constitution Principle II and leaks external models into domain entities.

### 1.5 Security & Multi-Tenant Isolation
- **Decision**: Multi-chain Spring Security 6 architecture with 3 ordered chains:
  - Chain 1 (`@Order(1)`): `/api/v1/events` and `/api/v1/coupons/**` authenticated via `ApiKeyFilter` evaluating header `X-API-KEY`. Resolves the `PosIntegration` and binds `tenantId` to the `SecurityContext`.
  - Chain 2 (`@Order(2)`): `/api/v1/**` BFF endpoints authenticated via JSON login (`POST /api/auth/login`), with `HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)` returning HTTP 401 (never redirecting to HTML login).
  - Chain 3 (`@Order(3)`): `/**` standard web portal with session/form login.
- **Rationale**: Mandated by Constitution Principle IV. Enforces tenant context at security filter boundary so business services never trust unauthenticated tenant IDs sent in request bodies.
- **Alternatives Considered**: Single filter chain with path matchers; rejected because REST API endpoints must return 401 while portal routes require 302 redirects.

### 1.6 Immutable Point Ledger & Balance Floor
- **Decision**: Point transactions are strictly append-only `PointTransaction` records (`EARN`, `REDEEM`, `EXPIRE`, `ADJUST`, `REVERSAL`). The current balance is cached on `LoyaltyAccount.pointsBalance` and updated atomically inside the aggregate root domain methods.
- **Rationale**: Guarantees zero negative balances (`pointsBalance >= 0` check inside `account.redeem()`), prevents double-spend via database row locking (`PESSIMISTIC_WRITE` or `@Version` optimistic locking), and provides complete reconciliation history.
- **Alternatives Considered**: Computing point balance on-the-fly via `SUM(amount)`; rejected because frequent balance checks at POS checkout require O(1) reads.

### 1.7 Automation Engine Design
- **Decision**: Parameterized rule engine using Strategy pattern for Triggers (`TriggerEvaluator`) and Actions (`ActionExecutor`).
- **Rationale**: Eliminates security risks associated with arbitrary expression languages (SpEL or MVEL) or dynamic scripting. Each trigger (`CUSTOMER_INACTIVE`, `FIRST_PURCHASE`, `BIRTHDAY`, `MILESTONE`) is a strongly-typed Java component that queries domain projections and dispatches predefined actions (`GENERATE_COUPON`, `SEND_EMAIL`).
- **Alternatives Considered**: Drools / BPMN engine (e.g., Camunda/Flowable); rejected in spec section 2.2 as unnecessary MVP complexity.
