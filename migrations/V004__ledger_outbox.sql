-- V004 immutable double-entry ledger, idempotency, reversals, and transactional outbox.

CREATE TABLE banking.posting_idempotency (
  tenant_id        uuid NOT NULL,
  source_system    text NOT NULL CHECK (length(trim(source_system)) > 0),
  idempotency_key  text NOT NULL CHECK (length(trim(idempotency_key)) > 0),
  request_hash     bytea NOT NULL,
  transaction_id   uuid,
  created_at       timestamptz NOT NULL DEFAULT clock_timestamp(),
  completed_at     timestamptz,
  expires_at       timestamptz,
  PRIMARY KEY ((tenant_id, source_system, idempotency_key) HASH),
  CONSTRAINT fk_posting_idempotency_tenant FOREIGN KEY (tenant_id) REFERENCES banking.tenant(tenant_id)
) WITH (COLOCATION = false) SPLIT INTO 32 TABLETS;

CREATE TABLE banking.ledger_transaction (
  tenant_id          uuid NOT NULL,
  transaction_id     uuid NOT NULL DEFAULT gen_random_uuid(),
  source_system      text NOT NULL CHECK (length(trim(source_system)) > 0),
  idempotency_key    text NOT NULL CHECK (length(trim(idempotency_key)) > 0),
  external_ref       text,
  transaction_type   text NOT NULL,
  status             text NOT NULL DEFAULT 'POSTED' CHECK (status IN ('POSTED')),
  description        text,
  effective_at       timestamptz NOT NULL,
  posted_at          timestamptz NOT NULL DEFAULT clock_timestamp(),
  metadata           jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_by         text NOT NULL DEFAULT current_user,
  PRIMARY KEY ((tenant_id, transaction_id) HASH),
  CONSTRAINT fk_ledger_transaction_tenant FOREIGN KEY (tenant_id) REFERENCES banking.tenant(tenant_id),
  CONSTRAINT fk_ledger_transaction_type FOREIGN KEY (transaction_type) REFERENCES banking.transaction_type(transaction_type_code),
  CONSTRAINT ux_ledger_transaction_idempotency UNIQUE (tenant_id, source_system, idempotency_key)
) WITH (COLOCATION = false) SPLIT INTO 64 TABLETS;

CREATE TABLE banking.ledger_entry (
  tenant_id        uuid NOT NULL,
  entry_id         uuid NOT NULL DEFAULT gen_random_uuid(),
  transaction_id   uuid NOT NULL,
  entry_no         integer NOT NULL CHECK (entry_no > 0),
  account_id       uuid NOT NULL,
  currency_code    banking.currency_code NOT NULL,
  direction        text NOT NULL CHECK (direction IN ('DEBIT', 'CREDIT')),
  amount_minor     numeric(38,0) NOT NULL CHECK (amount_minor > 0),
  narrative        text,
  effective_at     timestamptz NOT NULL,
  posted_at        timestamptz NOT NULL,
  created_at       timestamptz NOT NULL DEFAULT clock_timestamp(),
  PRIMARY KEY ((tenant_id, entry_id) HASH),
  CONSTRAINT fk_ledger_entry_transaction FOREIGN KEY (tenant_id, transaction_id) REFERENCES banking.ledger_transaction(tenant_id, transaction_id),
  CONSTRAINT fk_ledger_entry_account FOREIGN KEY (tenant_id, account_id) REFERENCES banking.account(tenant_id, account_id),
  CONSTRAINT fk_ledger_entry_currency FOREIGN KEY (currency_code) REFERENCES banking.currency(currency_code),
  CONSTRAINT ux_ledger_entry_tx_line UNIQUE (tenant_id, transaction_id, entry_no)
) WITH (COLOCATION = false) SPLIT INTO 96 TABLETS;

CREATE TABLE banking.ledger_reversal (
  tenant_id                uuid NOT NULL,
  original_transaction_id  uuid NOT NULL,
  reversal_transaction_id  uuid NOT NULL,
  reason                   text NOT NULL CHECK (length(trim(reason)) > 0),
  created_at               timestamptz NOT NULL DEFAULT clock_timestamp(),
  PRIMARY KEY ((tenant_id, original_transaction_id) HASH),
  CONSTRAINT fk_reversal_original FOREIGN KEY (tenant_id, original_transaction_id) REFERENCES banking.ledger_transaction(tenant_id, transaction_id),
  CONSTRAINT fk_reversal_reversal FOREIGN KEY (tenant_id, reversal_transaction_id) REFERENCES banking.ledger_transaction(tenant_id, transaction_id),
  CONSTRAINT ux_reversal_transaction UNIQUE (tenant_id, reversal_transaction_id),
  CONSTRAINT ck_reversal_not_self CHECK (original_transaction_id <> reversal_transaction_id)
) WITH (COLOCATION = false) SPLIT INTO 32 TABLETS;

CREATE TABLE banking.outbox_event (
  tenant_id       uuid NOT NULL,
  event_id        uuid NOT NULL DEFAULT gen_random_uuid(),
  aggregate_type  text NOT NULL CHECK (length(trim(aggregate_type)) > 0),
  aggregate_id    uuid NOT NULL,
  event_type      text NOT NULL CHECK (length(trim(event_type)) > 0),
  payload         jsonb NOT NULL,
  status          text NOT NULL DEFAULT 'PENDING'
                  CHECK (status IN ('PENDING', 'DISPATCHING', 'PUBLISHED', 'FAILED', 'DEAD')),
  attempts        integer NOT NULL DEFAULT 0 CHECK (attempts >= 0),
  max_attempts    integer NOT NULL DEFAULT 25 CHECK (max_attempts > 0),
  available_at    timestamptz NOT NULL DEFAULT clock_timestamp(),
  locked_by       text,
  locked_at       timestamptz,
  last_error      text,
  published_at    timestamptz,
  created_at      timestamptz NOT NULL DEFAULT clock_timestamp(),
  PRIMARY KEY ((tenant_id, event_id) HASH),
  CONSTRAINT fk_outbox_tenant FOREIGN KEY (tenant_id) REFERENCES banking.tenant(tenant_id)
) WITH (COLOCATION = false) SPLIT INTO 64 TABLETS;

ALTER TABLE banking.posting_idempotency
  ADD CONSTRAINT fk_posting_idempotency_transaction
  FOREIGN KEY (tenant_id, transaction_id) REFERENCES banking.ledger_transaction(tenant_id, transaction_id);

ALTER TABLE banking.account_balance
  ADD CONSTRAINT fk_account_balance_last_transaction
  FOREIGN KEY (tenant_id, last_transaction_id) REFERENCES banking.ledger_transaction(tenant_id, transaction_id);

CREATE INDEX idx_ledger_transaction_posted_at ON banking.ledger_transaction(tenant_id HASH, posted_at DESC) INCLUDE (transaction_id, transaction_type, source_system) SPLIT INTO 64 TABLETS;
CREATE INDEX idx_ledger_transaction_external_ref ON banking.ledger_transaction(source_system HASH, external_ref ASC) SPLIT INTO 32 TABLETS WHERE external_ref IS NOT NULL;
CREATE INDEX idx_ledger_entry_account_time ON banking.ledger_entry(account_id HASH, posted_at DESC) INCLUDE (tenant_id, transaction_id, direction, amount_minor, currency_code) SPLIT INTO 96 TABLETS;
CREATE INDEX idx_ledger_entry_transaction ON banking.ledger_entry(transaction_id HASH, entry_no ASC) INCLUDE (tenant_id, account_id, direction, amount_minor, currency_code) SPLIT INTO 64 TABLETS;
CREATE INDEX idx_outbox_pending ON banking.outbox_event(status HASH, available_at ASC, created_at ASC) SPLIT INTO 32 TABLETS WHERE status IN ('PENDING', 'FAILED');
CREATE INDEX idx_outbox_aggregate ON banking.outbox_event(aggregate_id HASH, created_at DESC) INCLUDE (tenant_id, event_type, status) SPLIT INTO 32 TABLETS;

CREATE TRIGGER zz_immutable_ledger_transaction
BEFORE UPDATE OR DELETE ON banking.ledger_transaction
FOR EACH ROW
EXECUTE FUNCTION banking.reject_immutable_change();

CREATE TRIGGER zz_immutable_ledger_entry
BEFORE UPDATE OR DELETE ON banking.ledger_entry
FOR EACH ROW
EXECUTE FUNCTION banking.reject_immutable_change();

CREATE TRIGGER zz_immutable_ledger_reversal
BEFORE UPDATE OR DELETE ON banking.ledger_reversal
FOR EACH ROW
EXECUTE FUNCTION banking.reject_immutable_change();

-- Defence-in-depth double-entry check.
-- The posting fn enforces DR=CR per currency before insert; this DEFERRED constraint
-- trigger re-validates at commit time, catching any future code path or break-glass
-- write that bypasses banking.post_ledger_transaction.
CREATE OR REPLACE FUNCTION banking.assert_ledger_double_entry()
RETURNS trigger
LANGUAGE plpgsql
SET search_path = pg_catalog, banking
AS $$
DECLARE
  v_unbalanced jsonb;
BEGIN
  WITH totals AS (
    SELECT currency_code,
           sum(CASE WHEN direction = 'DEBIT' THEN amount_minor ELSE 0 END) AS debits,
           sum(CASE WHEN direction = 'CREDIT' THEN amount_minor ELSE 0 END) AS credits,
           count(*) AS entry_count
    FROM banking.ledger_entry
    WHERE tenant_id = NEW.tenant_id
      AND transaction_id = NEW.transaction_id
    GROUP BY currency_code
    HAVING count(*) < 2
        OR sum(CASE WHEN direction = 'DEBIT' THEN amount_minor ELSE 0 END)
        <> sum(CASE WHEN direction = 'CREDIT' THEN amount_minor ELSE 0 END)
  )
  SELECT jsonb_agg(jsonb_build_object(
           'currency_code', currency_code,
           'debits', debits,
           'credits', credits,
           'entry_count', entry_count
         ))
    INTO v_unbalanced
  FROM totals;

  IF v_unbalanced IS NOT NULL THEN
    RAISE EXCEPTION 'double-entry violation on transaction %: %', NEW.transaction_id, v_unbalanced
      USING ERRCODE = '23514';
  END IF;

  RETURN NULL;
END;
$$;

CREATE CONSTRAINT TRIGGER trg_ledger_entry_double_entry
AFTER INSERT ON banking.ledger_entry
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION banking.assert_ledger_double_entry();

CREATE OR REPLACE FUNCTION banking.claim_outbox_events(
  p_consumer text,
  p_limit integer DEFAULT 100,
  p_lease_seconds integer DEFAULT 60
)
RETURNS SETOF banking.outbox_event
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, banking, public, pg_temp
AS $$
BEGIN
  IF p_consumer IS NULL OR length(trim(p_consumer)) = 0 THEN
    RAISE EXCEPTION 'p_consumer is required' USING ERRCODE = '22023';
  END IF;

  RETURN QUERY
  WITH picked AS (
    SELECT o.tenant_id, o.event_id
    FROM banking.outbox_event o
    WHERE o.status IN ('PENDING', 'FAILED')
      AND o.available_at <= clock_timestamp()
      AND (o.locked_at IS NULL OR o.locked_at < clock_timestamp() - ((greatest(p_lease_seconds, 1)::text || ' seconds')::interval))
    ORDER BY o.created_at ASC
    LIMIT greatest(1, least(coalesce(p_limit, 100), 1000))
    FOR UPDATE
  )
  UPDATE banking.outbox_event o
     SET status = 'DISPATCHING',
         locked_by = p_consumer,
         locked_at = clock_timestamp(),
         attempts = o.attempts + 1,
         last_error = NULL
    FROM picked p
   WHERE o.tenant_id = p.tenant_id
     AND o.event_id = p.event_id
  RETURNING o.*;
END;
$$;

CREATE OR REPLACE FUNCTION banking.mark_outbox_published(
  p_tenant_id uuid,
  p_event_id uuid,
  p_consumer text
)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, banking, public, pg_temp
AS $$
BEGIN
  UPDATE banking.outbox_event
     SET status = 'PUBLISHED',
         published_at = clock_timestamp(),
         locked_by = NULL,
         locked_at = NULL,
         last_error = NULL
   WHERE tenant_id = p_tenant_id
     AND event_id = p_event_id
     AND status = 'DISPATCHING'
     AND (p_consumer IS NULL OR locked_by = p_consumer);

  IF NOT FOUND THEN
    RAISE EXCEPTION 'outbox event % not claimable by consumer %', p_event_id, p_consumer USING ERRCODE = '02000';
  END IF;
END;
$$;

CREATE OR REPLACE FUNCTION banking.mark_outbox_failed(
  p_tenant_id uuid,
  p_event_id uuid,
  p_consumer text,
  p_error text
)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, banking, public, pg_temp
AS $$
BEGIN
  UPDATE banking.outbox_event
     SET status = CASE WHEN attempts >= max_attempts THEN 'DEAD' ELSE 'FAILED' END,
         available_at = CASE
           WHEN attempts >= max_attempts THEN available_at
           ELSE clock_timestamp() + (((power(2, least(attempts, 10))::integer)::text || ' seconds')::interval)
         END,
         locked_by = NULL,
         locked_at = NULL,
         last_error = left(coalesce(p_error, 'unknown'), 4000)
   WHERE tenant_id = p_tenant_id
     AND event_id = p_event_id
     AND status = 'DISPATCHING'
     AND (p_consumer IS NULL OR locked_by = p_consumer);

  IF NOT FOUND THEN
    RAISE EXCEPTION 'outbox event % not fail-markable by consumer %', p_event_id, p_consumer USING ERRCODE = '02000';
  END IF;
END;
$$;

COMMENT ON TABLE banking.ledger_transaction IS 'Immutable transaction header. The posting function enforces idempotency and balance rules.';
COMMENT ON TABLE banking.ledger_entry IS 'Immutable double-entry ledger lines. Sum of debits equals sum of credits per transaction and currency.';
COMMENT ON TABLE banking.outbox_event IS 'Transactional outbox for reliable downstream event publication.';
