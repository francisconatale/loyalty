# Quickstart & End-to-End Validation Guide: Loyalty Engine Core MVP

**Feature**: `001-core-loyalty-mvp`  
**Date**: 2026-11-21  
**Status**: Ready for Implementation

This guide provides end-to-end executable scenarios to validate the feature workflows without mocking database semantics.

---

## 1. Prerequisites & Environment Setup

- **Java**: 17+ (`java -version`)
- **Maven**: 3.8+ (`mvn -version`)
- **Database**: PostgreSQL 15+ running locally or in Docker:
  ```bash
  docker run --name loyalty-postgres -e POSTGRES_DB=loyalty_engine -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -p 5432:5432 -d postgres:16-alpine
  ```
- **App Startup**:
  ```bash
  mvn spring-boot:run
  ```

---

## 2. Validation Scenario 1: POS Event Ingestion with Optional Customer Upsert

### Step 1: Post a Purchase Event (with Optional Customer Profile)
```bash
curl -X POST http://localhost:8080/api/v1/events \
  -H "Content-Type: application/json" \
  -H "X-API-KEY: test-kioskito-api-key" \
  -d '{
    "type": "purchase.completed",
    "eventId": "evt-kioskito-1001",
    "occurredAt": "2026-11-21T15:30:00Z",
    "data": {
      "purchaseId": "sale-5001",
      "customerId": "kioskito-cust-001",
      "total": 35000.00,
      "currency": "ARS",
      "customer": {
        "name": "Maria Gonzalez",
        "email": "maria.gonzalez@example.com",
        "phone": "+5491144443333",
        "birthDate": "1992-08-20"
      }
    }
  }'
```
**Expected Outcome**: HTTP 202 Accepted within <500ms with `inboxId` and `status: "ACCEPTED"`. The optional `customer` block creates the canonical customer and its external reference on first sight. Later events for the same `customerId` may omit it (thin event).

### Step 2: Verify Point Accrual & Ledger Reconciliation
```bash
# Query customer loyalty balance (Customer ID resolved from the external reference)
curl -X GET http://localhost:8080/api/v1/customers/<CUSTOMER_ID>/loyalty \
  -H "Authorization: Bearer <ADMIN_OR_CUSTOMER_TOKEN>"
```
**Expected Outcome**: HTTP 200 OK. `pointsBalance: 350` (based on rule 1 pt per $100 spent).

```bash
curl -X GET http://localhost:8080/api/v1/customers/<CUSTOMER_ID>/transactions \
  -H "Authorization: Bearer <ADMIN_OR_CUSTOMER_TOKEN>"
```
**Expected Outcome**: HTTP 200 OK with 1 transaction of type `EARN`, `amount: 350`.

---

## 3. Validation Scenario 2: Single-Use Coupon Redemption at POS

### Step 1: Validate Coupon
```bash
curl -X POST http://localhost:8080/api/v1/coupons/validate \
  -H "Content-Type: application/json" \
  -H "X-API-KEY: test-kioskito-api-key" \
  -d '{
    "code": "DESC15-PROMO"
  }'
```
**Expected Outcome**: HTTP 200 OK with `isValid: true`, `status: "AVAILABLE"`, `discountValue: 15.0`.

### Step 2: Redeem Coupon
```bash
curl -X POST http://localhost:8080/api/v1/coupons/redeem \
  -H "Content-Type: application/json" \
  -H "X-API-KEY: test-kioskito-api-key" \
  -d '{
    "code": "DESC15-PROMO",
    "transactionReference": "sale-5002"
  }'
```
**Expected Outcome**: HTTP 200 OK with `status: "REDEEMED"`.

### Step 3: Prevent Replay Attack / Double Redemption
```bash
curl -X POST http://localhost:8080/api/v1/coupons/redeem \
  -H "Content-Type: application/json" \
  -H "X-API-KEY: test-kioskito-api-key" \
  -d '{
    "code": "DESC15-PROMO",
    "transactionReference": "sale-5003"
  }'
```
**Expected Outcome**: HTTP 409 Conflict with error message indicating coupon is already redeemed.

---

## 4. Validation Scenario 3: Automated Inactive Customer Re-Engagement

1. Insert or simulate a customer with `lastPurchaseAt` older than 30 days.
2. Trigger the automated evaluation worker:
   - Worker evaluates `CUSTOMER_INACTIVE` rule.
   - Evaluates condition `HAS_EMAIL == true`.
   - Generates a single-use coupon and persists `AutomationExecution` + `AutomationAction`.
   - Dispatches email via mock/transactional email adapter.
3. Query the customer's coupons to verify the new coupon exists with `status: "AVAILABLE"`.
4. Check that running the evaluation again does not generate duplicate coupons (idempotency key verification).
