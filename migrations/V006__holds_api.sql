-- V006 funds holds for card authorizations, delayed settlement, and operational reserves.

CREATE TABLE banking.funds_hold (
  tenant_id        uuid NOT NULL,
  hold_id          uuid NOT NULL DEFAULT gen_random_uuid(),
  account_id       uuid NOT NULL,
  source_system    text NOT NULL CHECK (length(trim(source_system)) > 0),
  idempotency_key  text NOT NULL CHECK (length(trim(idempotency_key)) > 0),
  external_ref     text,
  currency_code    banking.currency_code NOT NULL,
  amount_minor     numeric(38,0) NOT NULL CHECK (amount_minor > 0),
  status           text NOT NULL DEFAULT 'ACTIVE'
                   CHECK (status IN ('ACTIVE', 'RELEASED', 'EXPIRED', 'CAPTURED', 'CANCELLED')),
  reason           text,
  expires_at       timestamptz NOT NULL,
  released_at      timestamptz,
  release_reason   text,
  metadata         jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_by       text NOT NULL DEFAULT current_user,
  created_at       timestamptz NOT NULL DEFAULT clock_timestamp(),
  updated_at       timestamptz NOT NULL DEFAULT clock_timestamp(),
  PRIMARY KEY ((tenant_id, hold_id) HASH),
  CONSTRAINT fk_funds_hold_account FOREIGN KEY (tenant_id, account_id) REFERENCES banking.account(tenant_id, account_id),
  CONSTRAINT fk_funds_hold_currency FOREIGN KEY (currency_code) REFERENCES banking.currency(currency_code),
  CONSTRAINT ux_funds_hold_idempotency UNIQUE (tenant_id, source_system, idempotency_key)
) WITH (COLOCATION = false) SPLIT INTO 32 TABLETS;

CREATE INDEX idx_funds_hold_active_account ON banking.funds_hold(account_id HASH, expires_at ASC) SPLIT INTO 32 TABLETS WHERE status = 'ACTIVE';
CREATE INDEX idx_funds_hold_status_expiry ON banking.funds_hold(status HASH, expires_at ASC) SPLIT INTO 16 TABLETS WHERE status = 'ACTIVE';
CREATE INDEX idx_funds_hold_external_ref ON banking.funds_hold(source_system HASH, external_ref ASC) SPLIT INTO 16 TABLETS WHERE external_ref IS NOT NULL;

CREATE TRIGGER zz_funds_hold_touch_updated_at
BEFORE UPDATE ON banking.funds_hold
FOR EACH ROW
EXECUTE FUNCTION banking.touch_updated_at();

CREATE OR REPLACE FUNCTION banking.place_funds_hold(
  p_tenant_id uuid,
  p_account_id uuid,
  p_source_system text,
  p_idempotency_key text,
  p_amount_minor numeric,
  p_currency_code banking.currency_code,
  p_expires_at timestamptz,
  p_reason text DEFAULT NULL,
  p_external_ref text DEFAULT NULL,
  p_metadata jsonb DEFAULT '{}'::jsonb,
  p_created_by text DEFAULT NULL
)
RETURNS uuid
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, banking, public, pg_temp
AS $$
DECLARE
  v_now timestamptz := clock_timestamp();
  v_source_system text := trim(coalesce(p_source_system, ''));
  v_idempotency_key text := trim(coalesce(p_idempotency_key, ''));
  v_created_by text := coalesce(nullif(trim(coalesce(p_created_by, '')), ''), current_user);
  v_existing_hold_id uuid;
  v_hold_id uuid;
  v_currency banking.currency_code;
  v_available numeric(38,0);
  v_overdraft numeric(38,0);
  v_allow_debits boolean;
BEGIN
  IF current_setting('transaction_isolation') <> 'serializable' THEN
    RAISE EXCEPTION 'placing funds hold requires SERIALIZABLE transaction isolation; current isolation is %', current_setting('transaction_isolation')
      USING ERRCODE = '25001';
  END IF;

  IF p_tenant_id IS NULL OR p_account_id IS NULL THEN
    RAISE EXCEPTION 'tenant and account are required' USING ERRCODE = '22023';
  END IF;

  IF v_source_system = '' OR v_idempotency_key = '' THEN
    RAISE EXCEPTION 'source system and idempotency key are required' USING ERRCODE = '22023';
  END IF;

  IF p_amount_minor IS NULL OR p_amount_minor <= 0 OR p_amount_minor <> trunc(p_amount_minor) THEN
    RAISE EXCEPTION 'hold amount must be a positive integer minor-unit amount' USING ERRCODE = '23514';
  END IF;

  IF p_expires_at IS NULL OR p_expires_at <= v_now THEN
    RAISE EXCEPTION 'hold expiry must be in the future' USING ERRCODE = '23514';
  END IF;

  SELECT hold_id
    INTO v_existing_hold_id
  FROM banking.funds_hold
  WHERE tenant_id = p_tenant_id
    AND source_system = v_source_system
    AND idempotency_key = v_idempotency_key
  FOR UPDATE;

  IF v_existing_hold_id IS NOT NULL THEN
    RETURN v_existing_hold_id;
  END IF;

  SELECT a.currency_code, a.allow_debits, a.overdraft_limit_minor
    INTO v_currency, v_allow_debits, v_overdraft
  FROM banking.account a
  WHERE a.tenant_id = p_tenant_id
    AND a.account_id = p_account_id
    AND a.status = 'OPEN'
  FOR UPDATE;

  IF NOT FOUND THEN
    RAISE EXCEPTION 'account % is not open or does not exist', p_account_id USING ERRCODE = '23514';
  END IF;

  IF v_currency <> p_currency_code THEN
    RAISE EXCEPTION 'hold currency % does not match account currency %', p_currency_code, v_currency USING ERRCODE = '23514';
  END IF;

  IF NOT v_allow_debits THEN
    RAISE EXCEPTION 'debits are disabled for account %', p_account_id USING ERRCODE = '23514';
  END IF;

  SELECT b.available_balance_minor
    INTO v_available
  FROM banking.account_balance b
  WHERE b.tenant_id = p_tenant_id
    AND b.account_id = p_account_id
  FOR UPDATE;

  IF NOT FOUND THEN
    RAISE EXCEPTION 'balance row missing for account %', p_account_id USING ERRCODE = '23503';
  END IF;

  IF v_available + v_overdraft < p_amount_minor THEN
    RAISE EXCEPTION 'insufficient available funds for hold on account %', p_account_id USING ERRCODE = '23514';
  END IF;

  v_hold_id := gen_random_uuid();

  INSERT INTO banking.funds_hold(
    tenant_id,
    hold_id,
    account_id,
    source_system,
    idempotency_key,
    external_ref,
    currency_code,
    amount_minor,
    reason,
    expires_at,
    metadata,
    created_by
  ) VALUES (
    p_tenant_id,
    v_hold_id,
    p_account_id,
    v_source_system,
    v_idempotency_key,
    nullif(trim(coalesce(p_external_ref, '')), ''),
    p_currency_code,
    p_amount_minor::numeric(38,0),
    p_reason,
    p_expires_at,
    coalesce(p_metadata, '{}'::jsonb),
    v_created_by
  );

  UPDATE banking.account_balance
     SET held_balance_minor = held_balance_minor + p_amount_minor::numeric(38,0),
         version = version + 1,
         updated_at = v_now
   WHERE tenant_id = p_tenant_id
     AND account_id = p_account_id;

  INSERT INTO banking.outbox_event(tenant_id, aggregate_type, aggregate_id, event_type, payload)
  VALUES (
    p_tenant_id,
    'FundsHold',
    v_hold_id,
    'FundsHoldPlaced',
    jsonb_build_object(
      'tenant_id', p_tenant_id::text,
      'hold_id', v_hold_id::text,
      'account_id', p_account_id::text,
      'amount_minor', p_amount_minor::text,
      'currency_code', p_currency_code::text,
      'expires_at', p_expires_at
    )
  );

  RETURN v_hold_id;
END;
$$;

CREATE OR REPLACE FUNCTION banking.release_funds_hold(
  p_tenant_id uuid,
  p_hold_id uuid,
  p_release_reason text DEFAULT 'released',
  p_new_status text DEFAULT 'RELEASED'
)
RETURNS uuid
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, banking, public, pg_temp
AS $$
DECLARE
  v_now timestamptz := clock_timestamp();
  v_account_id uuid;
  v_amount numeric(38,0);
  v_status text;
  v_new_status text := upper(trim(coalesce(p_new_status, 'RELEASED')));
BEGIN
  IF current_setting('transaction_isolation') <> 'serializable' THEN
    RAISE EXCEPTION 'releasing funds hold requires SERIALIZABLE transaction isolation; current isolation is %', current_setting('transaction_isolation')
      USING ERRCODE = '25001';
  END IF;

  IF v_new_status NOT IN ('RELEASED', 'EXPIRED', 'CANCELLED') THEN
    RAISE EXCEPTION 'release status must be RELEASED, EXPIRED, or CANCELLED' USING ERRCODE = '23514';
  END IF;

  SELECT account_id, amount_minor, status
    INTO v_account_id, v_amount, v_status
  FROM banking.funds_hold
  WHERE tenant_id = p_tenant_id
    AND hold_id = p_hold_id
  FOR UPDATE;

  IF NOT FOUND THEN
    RAISE EXCEPTION 'funds hold % not found', p_hold_id USING ERRCODE = '02000';
  END IF;

  IF v_status <> 'ACTIVE' THEN
    RETURN p_hold_id;
  END IF;

  PERFORM 1
  FROM banking.account_balance
  WHERE tenant_id = p_tenant_id
    AND account_id = v_account_id
  FOR UPDATE;

  IF NOT FOUND THEN
    RAISE EXCEPTION 'balance row missing for account %', v_account_id USING ERRCODE = '23503';
  END IF;

  UPDATE banking.account_balance
     SET held_balance_minor = held_balance_minor - v_amount,
         version = version + 1,
         updated_at = v_now
   WHERE tenant_id = p_tenant_id
     AND account_id = v_account_id
     AND held_balance_minor >= v_amount;

  IF NOT FOUND THEN
    RAISE EXCEPTION 'held balance underflow for account % hold %', v_account_id, p_hold_id USING ERRCODE = '23514';
  END IF;

  UPDATE banking.funds_hold
     SET status = v_new_status,
         released_at = v_now,
         release_reason = coalesce(nullif(trim(coalesce(p_release_reason, '')), ''), lower(v_new_status))
   WHERE tenant_id = p_tenant_id
     AND hold_id = p_hold_id;

  INSERT INTO banking.outbox_event(tenant_id, aggregate_type, aggregate_id, event_type, payload)
  VALUES (
    p_tenant_id,
    'FundsHold',
    p_hold_id,
    'FundsHoldReleased',
    jsonb_build_object(
      'tenant_id', p_tenant_id::text,
      'hold_id', p_hold_id::text,
      'account_id', v_account_id::text,
      'amount_minor', v_amount::text,
      'status', v_new_status,
      'released_at', v_now
    )
  );

  RETURN p_hold_id;
END;
$$;

CREATE OR REPLACE FUNCTION banking.expire_due_funds_holds(
  p_tenant_id uuid,
  p_limit integer DEFAULT 100
)
RETURNS integer
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, banking, public, pg_temp
AS $$
DECLARE
  v_hold_id uuid;
  v_count integer := 0;
BEGIN
  IF current_setting('transaction_isolation') <> 'serializable' THEN
    RAISE EXCEPTION 'expiring funds holds requires SERIALIZABLE transaction isolation; current isolation is %', current_setting('transaction_isolation')
      USING ERRCODE = '25001';
  END IF;

  FOR v_hold_id IN
    SELECT hold_id
    FROM banking.funds_hold
    WHERE tenant_id = p_tenant_id
      AND status = 'ACTIVE'
      AND expires_at <= clock_timestamp()
    ORDER BY expires_at ASC
    LIMIT greatest(1, least(coalesce(p_limit, 100), 1000))
    FOR UPDATE
  LOOP
    PERFORM banking.release_funds_hold(p_tenant_id, v_hold_id, 'expired', 'EXPIRED');
    v_count := v_count + 1;
  END LOOP;

  RETURN v_count;
END;
$$;

COMMENT ON TABLE banking.funds_hold IS 'Active holds reduce available balance without changing ledger balance. Capture should be represented by a normal ledger transaction and then release/cancel the hold.';
