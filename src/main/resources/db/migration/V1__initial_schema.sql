-- V1__initial_schema.sql: Loyalty Engine Initial Database Schema

CREATE TABLE tenants (
    id UUID PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE pos_integrations (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    provider VARCHAR(64) NOT NULL,
    external_store_id VARCHAR(128) NOT NULL,
    api_key_hash VARCHAR(256) NOT NULL,
    encrypted_credentials TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_pos_integrations_provider_store UNIQUE (provider, external_store_id)
);

CREATE TABLE customers (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    name VARCHAR(128) NOT NULL,
    email VARCHAR(256),
    phone VARCHAR(64),
    birth_date DATE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_customers_tenant_email UNIQUE (tenant_id, email)
);

CREATE TABLE external_customer_references (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customers(id),
    integration_id UUID NOT NULL REFERENCES pos_integrations(id),
    external_id VARCHAR(128) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_ext_cust_ref_integration_external UNIQUE (integration_id, external_id)
);

CREATE TABLE purchases (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    customer_id UUID NOT NULL REFERENCES customers(id),
    integration_id UUID NOT NULL REFERENCES pos_integrations(id),
    external_id VARCHAR(128) NOT NULL,
    total NUMERIC(14,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(32) NOT NULL,
    purchased_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_purchases_integration_external UNIQUE (integration_id, external_id)
);

CREATE TABLE customer_stats (
    customer_id UUID PRIMARY KEY REFERENCES customers(id),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    total_purchases INTEGER NOT NULL DEFAULT 0,
    total_spent NUMERIC(14,2) NOT NULL DEFAULT 0.00,
    average_ticket NUMERIC(14,2) NOT NULL DEFAULT 0.00,
    first_purchase_at TIMESTAMPTZ,
    last_purchase_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE loyalty_programs (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    name VARCHAR(128) NOT NULL,
    type VARCHAR(32) NOT NULL,
    configuration JSONB NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE loyalty_accounts (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    customer_id UUID NOT NULL REFERENCES customers(id),
    program_id UUID NOT NULL REFERENCES loyalty_programs(id),
    points_balance BIGINT NOT NULL DEFAULT 0 CHECK (points_balance >= 0),
    lifetime_points BIGINT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_loyalty_accounts_program_customer UNIQUE (program_id, customer_id)
);

CREATE TABLE point_transactions (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES loyalty_accounts(id),
    type VARCHAR(32) NOT NULL,
    amount BIGINT NOT NULL,
    reference_id UUID,
    idempotency_key VARCHAR(128) NOT NULL,
    description VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_point_tx_account_idempotency UNIQUE (account_id, idempotency_key)
);

CREATE TABLE rewards (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    name VARCHAR(128) NOT NULL,
    description TEXT NOT NULL,
    points_cost BIGINT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE coupons (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    customer_id UUID NOT NULL REFERENCES customers(id),
    reward_id UUID REFERENCES rewards(id),
    code VARCHAR(64) NOT NULL,
    discount_type VARCHAR(32) NOT NULL,
    discount_value NUMERIC(14,2) NOT NULL,
    status VARCHAR(32) NOT NULL,
    expires_at TIMESTAMPTZ,
    redeemed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_coupons_tenant_code UNIQUE (tenant_id, code)
);

CREATE TABLE automations (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    name VARCHAR(128) NOT NULL,
    trigger_type VARCHAR(64) NOT NULL,
    configuration JSONB NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE automation_executions (
    id UUID PRIMARY KEY,
    automation_id UUID NOT NULL REFERENCES automations(id),
    customer_id UUID NOT NULL REFERENCES customers(id),
    trigger_event_id UUID,
    idempotency_key VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    triggered_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_auto_exec_automation_idempotency UNIQUE (automation_id, idempotency_key)
);

CREATE TABLE automation_actions (
    id UUID PRIMARY KEY,
    execution_id UUID NOT NULL REFERENCES automation_executions(id),
    action_type VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    result JSONB NOT NULL,
    executed_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE integration_events (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    integration_id UUID NOT NULL REFERENCES pos_integrations(id),
    external_event_id VARCHAR(128) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(32) NOT NULL,
    received_at TIMESTAMPTZ NOT NULL,
    processed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_integration_events_int_external UNIQUE (integration_id, external_event_id)
);

CREATE INDEX idx_customers_tenant ON customers(tenant_id);
CREATE INDEX idx_purchases_customer ON purchases(customer_id);
CREATE INDEX idx_purchases_tenant ON purchases(tenant_id);
CREATE INDEX idx_loyalty_accounts_customer ON loyalty_accounts(customer_id);
CREATE INDEX idx_point_tx_account ON point_transactions(account_id);
CREATE INDEX idx_coupons_customer ON coupons(customer_id);
CREATE INDEX idx_coupons_tenant_status ON coupons(tenant_id, status);
CREATE INDEX idx_integration_events_status ON integration_events(status);
