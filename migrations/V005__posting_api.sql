-- V005 posting API. All money movement should flow through these functions.

CREATE OR REPLACE FUNCTION banking.post_ledger_transaction(
  p_tenant_id uuid,
  p_source_system text,
  p_idempotency_key text,
  p_transaction_type text,
  p_entries jsonb,
  p_description text DEFAULT NULL,
  p_external_ref text DEFAULT NULL,
  p_effective_at timestamptz DEFAULT NULL,
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
  v_transaction_type text := upper(trim(coalesce(p_transaction_type, '')));
  v_external_ref text := nullif(trim(coalesce(p_external_ref, '')), '');
  v_effective_at timestamptz := coalesce(p_effective_at, clock_timestamp());
  v_created_by text := coalesce(nullif(trim(coalesce(p_created_by, '')), ''), banking.current_actor());
  v_request_id text := banking.current_request_id();
  v_request_hash bytea;
  v_existing_hash bytea;
  v_existing_transaction_id uuid;
  v_transaction_id uuid;
  v_entry_count integer;
  v_bad_count integer;
  v_delta_count integer;
  v_updated_count integer;
  v_unbalanced jsonb;
  v_account_id uuid;
  v_requires_external_ref boolean;
BEGIN
  IF current_setting('transaction_isolation') <> 'serializable' THEN
    RAISE EXCEPTION 'posting requires SERIALIZABLE transaction isolation; current isolation is %', current_setting('transaction_isolation')
      USING ERRCODE = '25001';
  END IF;

  IF p_tenant_id IS NULL THEN
    RAISE EXCEPTION 'p_tenant_id is required' USING ERRCODE = '22023';
  END IF;

  IF v_source_system = '' THEN
    RAISE EXCEPTION 'p_source_system is required' USING ERRCODE = '22023';
  END IF;

  IF v_idempotency_key = '' THEN
    RAISE EXCEPTION 'p_idempotency_key is required' USING ERRCODE = '22023';
  END IF;

  IF v_transaction_type = '' THEN
    RAISE EXCEPTION 'p_transaction_type is required' USING ERRCODE = '22023';
  END IF;

  IF p_entries IS NULL OR jsonb_typeof(p_entries) <> 'array' THEN
    RAISE EXCEPTION 'p_entries must be a JSON array' USING ERRCODE = '22023';
  END IF;

  SELECT tt.requires_external_ref
    INTO v_requires_external_ref
  FROM banking.transaction_type tt
  WHERE tt.transaction_type_code = v_transaction_type
    AND tt.enabled = true;

  IF v_requires_external_ref IS NULL THEN
    RAISE EXCEPTION 'transaction type % is not enabled or does not exist', v_transaction_type USING ERRCODE = '23503';
  END IF;

  IF v_requires_external_ref AND v_external_ref IS NULL THEN
    RAISE EXCEPTION 'transaction type % requires p_external_ref', v_transaction_type USING ERRCODE = '23514';
  END IF;

  SELECT count(*)
    INTO v_entry_count
  FROM jsonb_to_recordset(p_entries) AS x(account_id text, direction text, amount_minor text, currency_code text, narrative text);

  IF v_entry_count < 2 THEN
    RAISE EXCEPTION 'ledger transaction must contain at least two entries' USING ERRCODE = '23514';
  END IF;

  WITH parsed AS (
    SELECT row_number() OVER ()::integer AS entry_no,
           x.account_id::uuid AS account_id,
           upper(trim(coalesce(x.direction, ''))) AS direction,
           CASE WHEN trim(coalesce(x.amount_minor, '')) ~ '^[0-9]+$' THEN x.amount_minor::numeric(38,0) ELSE NULL END AS amount_minor,
           upper(trim(coalesce(x.currency_code, '')))::banking.currency_code AS currency_code,
           nullif(x.narrative, '') AS narrative
    FROM jsonb_to_recordset(p_entries) AS x(account_id text, direction text, amount_minor text, currency_code text, narrative text)
  )
  SELECT count(*)
    INTO v_bad_count
  FROM parsed
  WHERE account_id IS NULL
     OR direction NOT IN ('DEBIT', 'CREDIT')
     OR amount_minor IS NULL
     OR amount_minor <= 0
     OR currency_code IS NULL;

  IF v_bad_count > 0 THEN
    RAISE EXCEPTION 'one or more ledger entries are invalid' USING ERRCODE = '23514';
  END IF;

  WITH parsed AS (
    SELECT x.account_id::uuid AS account_id,
           upper(trim(coalesce(x.direction, ''))) AS direction,
           CASE WHEN trim(coalesce(x.amount_minor, '')) ~ '^[0-9]+$' THEN x.amount_minor::numeric(38,0) ELSE NULL END AS amount_minor,
           upper(trim(coalesce(x.currency_code, '')))::banking.currency_code AS currency_code
    FROM jsonb_to_recordset(p_entries) AS x(account_id text, direction text, amount_minor text, currency_code text, narrative text)
  ), totals AS (
    SELECT currency_code,
           sum(CASE WHEN direction = 'DEBIT' THEN amount_minor ELSE 0 END) AS debits,
           sum(CASE WHEN direction = 'CREDIT' THEN amount_minor ELSE 0 END) AS credits
    FROM parsed
    GROUP BY currency_code
    HAVING sum(CASE WHEN direction = 'DEBIT' THEN amount_minor ELSE 0 END)
        <> sum(CASE WHEN direction = 'CREDIT' THEN amount_minor ELSE 0 END)
  )
  SELECT jsonb_agg(jsonb_build_object('currency_code', currency_code, 'debits', debits, 'credits', credits))
    INTO v_unbalanced
  FROM totals;

  IF v_unbalanced IS NOT NULL THEN
    RAISE EXCEPTION 'ledger transaction is not balanced: %', v_unbalanced USING ERRCODE = '23514';
  END IF;

  WITH parsed AS (
    SELECT x.account_id::uuid AS account_id,
           upper(trim(coalesce(x.direction, ''))) AS direction,
           upper(trim(coalesce(x.currency_code, '')))::banking.currency_code AS currency_code
    FROM jsonb_to_recordset(p_entries) AS x(account_id text, direction text, amount_minor text, currency_code text, narrative text)
  )
  SELECT count(*)
    INTO v_bad_count
  FROM parsed e
  LEFT JOIN banking.account a
    ON a.tenant_id = p_tenant_id
   AND a.account_id = e.account_id
  WHERE a.account_id IS NULL
     OR a.status <> 'OPEN'
     OR a.currency_code <> e.currency_code
     OR (e.direction = 'DEBIT' AND a.allow_debits = false)
     OR (e.direction = 'CREDIT' AND a.allow_credits = false);

  IF v_bad_count > 0 THEN
    RAISE EXCEPTION 'one or more accounts are missing, closed/frozen, currency-mismatched, or direction-blocked' USING ERRCODE = '23514';
  END IF;

  v_request_hash := digest(
    coalesce(p_tenant_id::text, '') || chr(31) ||
    v_source_system || chr(31) ||
    v_idempotency_key || chr(31) ||
    v_transaction_type || chr(31) ||
    coalesce(v_external_ref, '') || chr(31) ||
    coalesce(p_description, '') || chr(31) ||
    coalesce(v_effective_at::text, '') || chr(31) ||
    coalesce(p_entries::text, '') || chr(31) ||
    coalesce(p_metadata::text, '{}'),
    'sha256'
  );

  INSERT INTO banking.posting_idempotency(tenant_id, source_system, idempotency_key, request_hash, created_at)
  VALUES (p_tenant_id, v_source_system, v_idempotency_key, v_request_hash, v_now)
  ON CONFLICT DO NOTHING;

  SELECT request_hash, transaction_id
    INTO v_existing_hash, v_existing_transaction_id
  FROM banking.posting_idempotency
  WHERE tenant_id = p_tenant_id
    AND source_system = v_source_system
    AND idempotency_key = v_idempotency_key
  FOR UPDATE;

  IF NOT FOUND THEN
    RAISE EXCEPTION 'failed to acquire idempotency key %, source %', v_idempotency_key, v_source_system USING ERRCODE = '40001';
  END IF;

  IF v_existing_hash IS DISTINCT FROM v_request_hash THEN
    RAISE EXCEPTION 'idempotency key % for source % was reused with a different request', v_idempotency_key, v_source_system
      USING ERRCODE = '23505';
  END IF;

  IF v_existing_transaction_id IS NOT NULL THEN
    RETURN v_existing_transaction_id;
  END IF;

  -- Deterministic row locks reduce deadlock risk when two postings touch overlapping accounts.
  FOR v_account_id IN
    SELECT DISTINCT x.account_id::uuid AS account_id
    FROM jsonb_to_recordset(p_entries) AS x(account_id text, direction text, amount_minor text, currency_code text, narrative text)
    ORDER BY account_id
  LOOP
    PERFORM 1
    FROM banking.account
    WHERE tenant_id = p_tenant_id
      AND account_id = v_account_id
      AND status = 'OPEN'
    FOR UPDATE;

    IF NOT FOUND THEN
      RAISE EXCEPTION 'account % is not open or does not exist', v_account_id USING ERRCODE = '23514';
    END IF;

    PERFORM 1
    FROM banking.account_balance
    WHERE tenant_id = p_tenant_id
      AND account_id = v_account_id
    FOR UPDATE;

    IF NOT FOUND THEN
      RAISE EXCEPTION 'balance row missing for account %', v_account_id USING ERRCODE = '23503';
    END IF;
  END LOOP;

  v_transaction_id := gen_random_uuid();

  INSERT INTO banking.ledger_transaction(
    tenant_id,
    transaction_id,
    source_system,
    idempotency_key,
    external_ref,
    transaction_type,
    description,
    effective_at,
    posted_at,
    metadata,
    created_by
  ) VALUES (
    p_tenant_id,
    v_transaction_id,
    v_source_system,
    v_idempotency_key,
    v_external_ref,
    v_transaction_type,
    p_description,
    v_effective_at,
    v_now,
    coalesce(p_metadata, '{}'::jsonb),
    v_created_by
  );

  WITH parsed AS (
    SELECT row_number() OVER ()::integer AS entry_no,
           x.account_id::uuid AS account_id,
           upper(trim(coalesce(x.direction, ''))) AS direction,
           x.amount_minor::numeric(38,0) AS amount_minor,
           upper(trim(coalesce(x.currency_code, '')))::banking.currency_code AS currency_code,
           nullif(x.narrative, '') AS narrative
    FROM jsonb_to_recordset(p_entries) AS x(account_id text, direction text, amount_minor text, currency_code text, narrative text)
  )
  INSERT INTO banking.ledger_entry(
    tenant_id,
    transaction_id,
    entry_no,
    account_id,
    currency_code,
    direction,
    amount_minor,
    narrative,
    effective_at,
    posted_at
  )
  SELECT p_tenant_id,
         v_transaction_id,
         entry_no,
         account_id,
         currency_code,
         direction,
         amount_minor,
         narrative,
         v_effective_at,
         v_now
  FROM parsed
  ORDER BY entry_no;

  WITH parsed AS (
    SELECT x.account_id::uuid AS account_id,
           upper(trim(coalesce(x.direction, ''))) AS direction,
           x.amount_minor::numeric(38,0) AS amount_minor
    FROM jsonb_to_recordset(p_entries) AS x(account_id text, direction text, amount_minor text, currency_code text, narrative text)
  ), deltas AS (
    SELECT e.account_id,
           sum(CASE WHEN e.direction = a.normal_balance THEN e.amount_minor ELSE -e.amount_minor END) AS delta_minor
    FROM parsed e
    JOIN banking.account a
      ON a.tenant_id = p_tenant_id
     AND a.account_id = e.account_id
    GROUP BY e.account_id
  )
  SELECT count(*)
    INTO v_delta_count
  FROM deltas;

  WITH parsed AS (
    SELECT x.account_id::uuid AS account_id,
           upper(trim(coalesce(x.direction, ''))) AS direction,
           x.amount_minor::numeric(38,0) AS amount_minor
    FROM jsonb_to_recordset(p_entries) AS x(account_id text, direction text, amount_minor text, currency_code text, narrative text)
  ), deltas AS (
    SELECT e.account_id,
           sum(CASE WHEN e.direction = a.normal_balance THEN e.amount_minor ELSE -e.amount_minor END) AS delta_minor
    FROM parsed e
    JOIN banking.account a
      ON a.tenant_id = p_tenant_id
     AND a.account_id = e.account_id
    GROUP BY e.account_id
  ), eligible AS (
    SELECT d.account_id, d.delta_minor
    FROM deltas d
    JOIN banking.account a
      ON a.tenant_id = p_tenant_id
     AND a.account_id = d.account_id
    JOIN banking.account_balance b
      ON b.tenant_id = p_tenant_id
     AND b.account_id = d.account_id
    WHERE a.allow_negative_balance = true
       OR (b.ledger_balance_minor + d.delta_minor + a.overdraft_limit_minor) >= 0
  )
  UPDATE banking.account_balance b
     SET ledger_balance_minor = b.ledger_balance_minor + e.delta_minor,
         version = b.version + 1,
         last_transaction_id = v_transaction_id,
         updated_at = v_now
    FROM eligible e
   WHERE b.tenant_id = p_tenant_id
     AND b.account_id = e.account_id;

  GET DIAGNOSTICS v_updated_count = ROW_COUNT;

  IF v_updated_count <> v_delta_count THEN
    RAISE EXCEPTION 'insufficient available funds or balance update conflict for transaction %', v_transaction_id
      USING ERRCODE = '23514';
  END IF;

  INSERT INTO banking.outbox_event(
    tenant_id,
    aggregate_type,
    aggregate_id,
    event_type,
    payload
  ) VALUES (
    p_tenant_id,
    'LedgerTransaction',
    v_transaction_id,
    'LedgerTransactionPosted',
    jsonb_build_object(
      'tenant_id', p_tenant_id::text,
      'transaction_id', v_transaction_id::text,
      'source_system', v_source_system,
      'idempotency_key', v_idempotency_key,
      'transaction_type', v_transaction_type,
      'external_ref', v_external_ref,
      'posted_at', v_now,
      'effective_at', v_effective_at,
      'created_by', v_created_by,
      'request_id', v_request_id,
      'entries', p_entries
    )
  );

  UPDATE banking.posting_idempotency
     SET transaction_id = v_transaction_id,
         completed_at = v_now
   WHERE tenant_id = p_tenant_id
     AND source_system = v_source_system
     AND idempotency_key = v_idempotency_key;

  RETURN v_transaction_id;
END;
$$;

CREATE OR REPLACE FUNCTION banking.reverse_ledger_transaction(
  p_tenant_id uuid,
  p_original_transaction_id uuid,
  p_source_system text,
  p_idempotency_key text,
  p_reason text,
  p_metadata jsonb DEFAULT '{}'::jsonb,
  p_created_by text DEFAULT NULL
)
RETURNS uuid
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, banking, public, pg_temp
AS $$
DECLARE
  v_entries jsonb;
  v_existing_reversal_id uuid;
  v_reversal_transaction_id uuid;
BEGIN
  IF current_setting('transaction_isolation') <> 'serializable' THEN
    RAISE EXCEPTION 'reversal requires SERIALIZABLE transaction isolation; current isolation is %', current_setting('transaction_isolation')
      USING ERRCODE = '25001';
  END IF;

  IF p_reason IS NULL OR length(trim(p_reason)) = 0 THEN
    RAISE EXCEPTION 'p_reason is required' USING ERRCODE = '22023';
  END IF;

  PERFORM 1
  FROM banking.ledger_transaction
  WHERE tenant_id = p_tenant_id
    AND transaction_id = p_original_transaction_id
  FOR UPDATE;

  IF NOT FOUND THEN
    RAISE EXCEPTION 'original transaction % not found', p_original_transaction_id USING ERRCODE = '02000';
  END IF;

  SELECT reversal_transaction_id
    INTO v_existing_reversal_id
  FROM banking.ledger_reversal
  WHERE tenant_id = p_tenant_id
    AND original_transaction_id = p_original_transaction_id;

  IF v_existing_reversal_id IS NOT NULL THEN
    RETURN v_existing_reversal_id;
  END IF;

  SELECT jsonb_agg(
           jsonb_build_object(
             'account_id', account_id::text,
             'direction', CASE WHEN direction = 'DEBIT' THEN 'CREDIT' ELSE 'DEBIT' END,
             'amount_minor', amount_minor::text,
             'currency_code', currency_code::text,
             'narrative', 'Reversal of ' || p_original_transaction_id::text
           ) ORDER BY entry_no
         )
    INTO v_entries
  FROM banking.ledger_entry
  WHERE tenant_id = p_tenant_id
    AND transaction_id = p_original_transaction_id;

  IF v_entries IS NULL OR jsonb_array_length(v_entries) = 0 THEN
    RAISE EXCEPTION 'original transaction % has no entries', p_original_transaction_id USING ERRCODE = '23514';
  END IF;

  v_reversal_transaction_id := banking.post_ledger_transaction(
    p_tenant_id        => p_tenant_id,
    p_source_system    => p_source_system,
    p_idempotency_key  => p_idempotency_key,
    p_transaction_type => 'REVERSAL',
    p_entries          => v_entries,
    p_description      => 'Reversal: ' || trim(p_reason),
    p_external_ref     => p_original_transaction_id::text,
    p_effective_at     => clock_timestamp(),
    p_metadata         => coalesce(p_metadata, '{}'::jsonb) || jsonb_build_object('original_transaction_id', p_original_transaction_id::text),
    p_created_by       => p_created_by
  );

  INSERT INTO banking.ledger_reversal(
    tenant_id,
    original_transaction_id,
    reversal_transaction_id,
    reason
  ) VALUES (
    p_tenant_id,
    p_original_transaction_id,
    v_reversal_transaction_id,
    trim(p_reason)
  );

  RETURN v_reversal_transaction_id;
END;
$$;

COMMENT ON FUNCTION banking.post_ledger_transaction IS 'Posts one balanced, idempotent, immutable double-entry transaction and updates balance cache in the same serializable transaction.';
COMMENT ON FUNCTION banking.reverse_ledger_transaction IS 'Posts an exact opposite transaction and records a one-time reversal link.';
