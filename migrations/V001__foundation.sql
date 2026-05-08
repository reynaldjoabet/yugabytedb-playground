-- V001 foundation: schemas, extensions, common domains, tenant, reference tables, common triggers.
-- Target: YugabyteDB YSQL.

CREATE SCHEMA IF NOT EXISTS banking;
CREATE SCHEMA IF NOT EXISTS audit;
CREATE SCHEMA IF NOT EXISTS ops;

-- Required for digest(..., 'sha256') idempotency hashes. gen_random_uuid() is built in to YSQL.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Hardening for SECURITY DEFINER functions that may reference extension functions in public.
REVOKE CREATE ON SCHEMA public FROM PUBLIC;

CREATE DOMAIN banking.currency_code AS char(3)
  CHECK (VALUE ~ '^[A-Z]{3}$');

CREATE DOMAIN banking.amount_minor AS numeric(38,0)
  CHECK (VALUE >= 0);

CREATE DOMAIN banking.signed_amount_minor AS numeric(38,0);

CREATE TABLE banking.tenant (
  tenant_id       uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_code     text NOT NULL UNIQUE CHECK (tenant_code ~ '^[A-Z0-9_:-]{3,64}$'),
  tenant_name     text NOT NULL CHECK (length(trim(tenant_name)) > 0),
  home_country    char(2) NOT NULL CHECK (home_country ~ '^[A-Z]{2}$'),
  status          text NOT NULL DEFAULT 'ACTIVE'
                  CHECK (status IN ('ACTIVE', 'SUSPENDED', 'CLOSED')),
  metadata        jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at      timestamptz NOT NULL DEFAULT clock_timestamp(),
  updated_at      timestamptz NOT NULL DEFAULT clock_timestamp()
);

CREATE TABLE banking.currency (
  currency_code   banking.currency_code PRIMARY KEY,
  numeric_code    char(3) UNIQUE CHECK (numeric_code IS NULL OR numeric_code ~ '^[0-9]{3}$'),
  exponent        smallint NOT NULL CHECK (exponent BETWEEN 0 AND 8),
  currency_name   text NOT NULL,
  enabled         boolean NOT NULL DEFAULT true,
  created_at      timestamptz NOT NULL DEFAULT clock_timestamp()
);

CREATE TABLE banking.account_category (
  account_category_code text PRIMARY KEY,
  normal_balance        text NOT NULL CHECK (normal_balance IN ('DEBIT', 'CREDIT')),
  description           text NOT NULL
);

CREATE TABLE banking.transaction_type (
  transaction_type_code text PRIMARY KEY,
  description           text NOT NULL,
  enabled               boolean NOT NULL DEFAULT true,
  requires_external_ref boolean NOT NULL DEFAULT false
);

CREATE TABLE audit.audit_event (
  audit_event_id uuid NOT NULL DEFAULT gen_random_uuid(),
  tenant_id      uuid,
  actor          text NOT NULL DEFAULT current_user,
  request_id     text,
  action         text NOT NULL,
  entity_schema  text NOT NULL,
  entity_table   text NOT NULL,
  entity_key     jsonb NOT NULL,
  before_row     jsonb,
  after_row      jsonb,
  created_at     timestamptz NOT NULL DEFAULT clock_timestamp(),
  PRIMARY KEY (audit_event_id HASH)
) WITH (COLOCATION = false) SPLIT INTO 8 TABLETS;

CREATE INDEX idx_audit_event_tenant_time ON audit.audit_event(tenant_id HASH, created_at DESC) SPLIT INTO 8 TABLETS;
CREATE INDEX idx_audit_event_entity ON audit.audit_event(entity_schema HASH, entity_table ASC, created_at DESC) SPLIT INTO 8 TABLETS;
CREATE INDEX idx_audit_event_request ON audit.audit_event(request_id HASH) SPLIT INTO 4 TABLETS WHERE request_id IS NOT NULL;

CREATE OR REPLACE FUNCTION banking.touch_updated_at()
RETURNS trigger
LANGUAGE plpgsql
SET search_path = pg_catalog, banking
AS $$
BEGIN
  NEW.updated_at := clock_timestamp();
  RETURN NEW;
END;
$$;

-- Session context: caller-supplied identity, request correlation, and tenant scope.
-- Applications call this once per request/transaction so that audit rows, ledger
-- transactions, and outbox events all carry the originating actor and request id.
CREATE OR REPLACE FUNCTION banking.set_session_context(
  p_actor text,
  p_request_id text,
  p_tenant_id uuid
)
RETURNS void
LANGUAGE plpgsql
AS $$
BEGIN
  PERFORM set_config('app.actor',      coalesce(nullif(trim(coalesce(p_actor, '')), ''), ''), false);
  PERFORM set_config('app.request_id', coalesce(nullif(trim(coalesce(p_request_id, '')), ''), ''), false);
  PERFORM set_config('app.tenant_id',  coalesce(p_tenant_id::text, ''), false);
END;
$$;

CREATE OR REPLACE FUNCTION banking.current_actor()
RETURNS text
LANGUAGE sql
STABLE
AS $$
  SELECT coalesce(nullif(current_setting('app.actor', true), ''), session_user);
$$;

CREATE OR REPLACE FUNCTION banking.current_request_id()
RETURNS text
LANGUAGE sql
STABLE
AS $$
  SELECT nullif(current_setting('app.request_id', true), '');
$$;

CREATE OR REPLACE FUNCTION banking.current_tenant_id()
RETURNS uuid
LANGUAGE sql
STABLE
AS $$
  SELECT nullif(current_setting('app.tenant_id', true), '')::uuid;
$$;

CREATE OR REPLACE FUNCTION banking.reject_immutable_change()
RETURNS trigger
LANGUAGE plpgsql
SET search_path = pg_catalog, banking
AS $$
BEGIN
  RAISE EXCEPTION 'immutable table %.% cannot be updated or deleted', TG_TABLE_SCHEMA, TG_TABLE_NAME
    USING ERRCODE = '55000';
END;
$$;

CREATE OR REPLACE FUNCTION audit.capture_row_change()
RETURNS trigger
LANGUAGE plpgsql
SET search_path = pg_catalog, audit, banking
AS $$
DECLARE
  v_tenant_id uuid;
  v_entity_key jsonb;
  v_actor text := banking.current_actor();
  v_request_id text := banking.current_request_id();
BEGIN
  IF TG_OP = 'INSERT' THEN
    v_tenant_id := CASE WHEN to_jsonb(NEW) ? 'tenant_id' THEN (to_jsonb(NEW)->>'tenant_id')::uuid ELSE NULL END;
    v_entity_key := jsonb_build_object('op', TG_OP, 'row', to_jsonb(NEW));
    INSERT INTO audit.audit_event(tenant_id, actor, request_id, action, entity_schema, entity_table, entity_key, after_row)
    VALUES (v_tenant_id, v_actor, v_request_id, TG_OP, TG_TABLE_SCHEMA, TG_TABLE_NAME, v_entity_key, to_jsonb(NEW));
    RETURN NEW;
  ELSIF TG_OP = 'UPDATE' THEN
    v_tenant_id := CASE WHEN to_jsonb(NEW) ? 'tenant_id' THEN (to_jsonb(NEW)->>'tenant_id')::uuid ELSE NULL END;
    v_entity_key := jsonb_build_object('op', TG_OP, 'old', to_jsonb(OLD), 'new', to_jsonb(NEW));
    INSERT INTO audit.audit_event(tenant_id, actor, request_id, action, entity_schema, entity_table, entity_key, before_row, after_row)
    VALUES (v_tenant_id, v_actor, v_request_id, TG_OP, TG_TABLE_SCHEMA, TG_TABLE_NAME, v_entity_key, to_jsonb(OLD), to_jsonb(NEW));
    RETURN NEW;
  ELSIF TG_OP = 'DELETE' THEN
    v_tenant_id := CASE WHEN to_jsonb(OLD) ? 'tenant_id' THEN (to_jsonb(OLD)->>'tenant_id')::uuid ELSE NULL END;
    v_entity_key := jsonb_build_object('op', TG_OP, 'row', to_jsonb(OLD));
    INSERT INTO audit.audit_event(tenant_id, actor, request_id, action, entity_schema, entity_table, entity_key, before_row)
    VALUES (v_tenant_id, v_actor, v_request_id, TG_OP, TG_TABLE_SCHEMA, TG_TABLE_NAME, v_entity_key, to_jsonb(OLD));
    RETURN OLD;
  END IF;

  RETURN NULL;
END;
$$;

CREATE TRIGGER zz_tenant_touch_updated_at
BEFORE UPDATE ON banking.tenant
FOR EACH ROW
EXECUTE FUNCTION banking.touch_updated_at();

COMMENT ON SCHEMA banking IS 'Core banking operational schema.';
COMMENT ON DOMAIN banking.amount_minor IS 'Exact integer minor-unit amount, for example cents, paise, pence. Never use floating point for money.';
COMMENT ON TABLE banking.tenant IS 'Logical bank/institution/tenant boundary.';
COMMENT ON TABLE audit.audit_event IS 'Append-only audit trail for mutable reference/onboarding tables. Ledger facts are immutable separately.';
COMMENT ON FUNCTION banking.set_session_context IS 'Bind actor + request id + tenant id to the current session via app.* GUCs. Call once per request/transaction; values flow into audit, ledger, and outbox metadata.';
