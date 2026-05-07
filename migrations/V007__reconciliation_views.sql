-- V007 reconciliation and integrity views. Use these in batch controls, not on hot request paths.

CREATE OR REPLACE VIEW banking.v_transaction_balance_violations AS
SELECT
  tenant_id,
  transaction_id,
  currency_code,
  sum(CASE WHEN direction = 'DEBIT' THEN amount_minor ELSE 0 END) AS debit_minor,
  sum(CASE WHEN direction = 'CREDIT' THEN amount_minor ELSE 0 END) AS credit_minor,
  sum(CASE WHEN direction = 'DEBIT' THEN amount_minor ELSE -amount_minor END) AS imbalance_minor,
  count(*) AS entry_count
FROM banking.ledger_entry
GROUP BY tenant_id, transaction_id, currency_code
HAVING count(*) < 2
    OR sum(CASE WHEN direction = 'DEBIT' THEN amount_minor ELSE 0 END)
    <> sum(CASE WHEN direction = 'CREDIT' THEN amount_minor ELSE 0 END);

CREATE OR REPLACE VIEW banking.v_account_balance_rebuild AS
SELECT
  a.tenant_id,
  a.account_id,
  a.account_no,
  a.account_category_code,
  a.normal_balance,
  a.currency_code,
  coalesce(sum(CASE WHEN e.direction = a.normal_balance THEN e.amount_minor ELSE -e.amount_minor END), 0)::numeric(38,0) AS rebuilt_ledger_balance_minor,
  count(e.entry_id) AS entry_count,
  max(e.posted_at) AS last_entry_at
FROM banking.account a
LEFT JOIN banking.ledger_entry e
  ON e.tenant_id = a.tenant_id
 AND e.account_id = a.account_id
GROUP BY a.tenant_id, a.account_id, a.account_no, a.account_category_code, a.normal_balance, a.currency_code;

CREATE OR REPLACE VIEW banking.v_account_balance_drift AS
SELECT
  r.tenant_id,
  r.account_id,
  r.account_no,
  r.currency_code,
  r.rebuilt_ledger_balance_minor,
  b.ledger_balance_minor AS cached_ledger_balance_minor,
  b.held_balance_minor AS cached_held_balance_minor,
  b.available_balance_minor AS cached_available_balance_minor,
  (b.ledger_balance_minor - r.rebuilt_ledger_balance_minor)::numeric(38,0) AS drift_minor,
  b.version,
  b.last_transaction_id,
  b.updated_at AS balance_updated_at,
  r.last_entry_at
FROM banking.v_account_balance_rebuild r
JOIN banking.account_balance b
  ON b.tenant_id = r.tenant_id
 AND b.account_id = r.account_id
WHERE b.currency_code <> r.currency_code
   OR b.ledger_balance_minor <> r.rebuilt_ledger_balance_minor;

CREATE OR REPLACE VIEW banking.v_active_holds_rebuild AS
SELECT
  a.tenant_id,
  a.account_id,
  a.account_no,
  a.currency_code,
  coalesce(sum(h.amount_minor) FILTER (WHERE h.status = 'ACTIVE'), 0)::numeric(38,0) AS rebuilt_held_balance_minor,
  count(h.hold_id) FILTER (WHERE h.status = 'ACTIVE') AS active_hold_count,
  min(h.expires_at) FILTER (WHERE h.status = 'ACTIVE') AS next_hold_expiry_at
FROM banking.account a
LEFT JOIN banking.funds_hold h
  ON h.tenant_id = a.tenant_id
 AND h.account_id = a.account_id
GROUP BY a.tenant_id, a.account_id, a.account_no, a.currency_code;

CREATE OR REPLACE VIEW banking.v_hold_balance_drift AS
SELECT
  r.tenant_id,
  r.account_id,
  r.account_no,
  r.currency_code,
  r.rebuilt_held_balance_minor,
  b.held_balance_minor AS cached_held_balance_minor,
  (b.held_balance_minor - r.rebuilt_held_balance_minor)::numeric(38,0) AS drift_minor,
  b.version,
  b.updated_at AS balance_updated_at,
  r.next_hold_expiry_at
FROM banking.v_active_holds_rebuild r
JOIN banking.account_balance b
  ON b.tenant_id = r.tenant_id
 AND b.account_id = r.account_id
WHERE b.held_balance_minor <> r.rebuilt_held_balance_minor;

CREATE OR REPLACE VIEW banking.v_trial_balance_by_currency AS
SELECT
  e.tenant_id,
  e.currency_code,
  sum(CASE WHEN e.direction = 'DEBIT' THEN e.amount_minor ELSE 0 END)::numeric(38,0) AS debit_minor,
  sum(CASE WHEN e.direction = 'CREDIT' THEN e.amount_minor ELSE 0 END)::numeric(38,0) AS credit_minor,
  sum(CASE WHEN e.direction = 'DEBIT' THEN e.amount_minor ELSE -e.amount_minor END)::numeric(38,0) AS net_minor,
  count(DISTINCT e.transaction_id) AS transaction_count,
  count(*) AS entry_count
FROM banking.ledger_entry e
GROUP BY e.tenant_id, e.currency_code;

CREATE OR REPLACE FUNCTION banking.assert_reconciliation_clean(p_tenant_id uuid DEFAULT NULL)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, banking, public, pg_temp
AS $$
DECLARE
  v_count integer;
BEGIN
  SELECT count(*) INTO v_count
  FROM banking.v_transaction_balance_violations
  WHERE p_tenant_id IS NULL OR tenant_id = p_tenant_id;

  IF v_count > 0 THEN
    RAISE EXCEPTION 'transaction balance violations found: %', v_count USING ERRCODE = '23514';
  END IF;

  SELECT count(*) INTO v_count
  FROM banking.v_account_balance_drift
  WHERE p_tenant_id IS NULL OR tenant_id = p_tenant_id;

  IF v_count > 0 THEN
    RAISE EXCEPTION 'account ledger balance drift rows found: %', v_count USING ERRCODE = '23514';
  END IF;

  SELECT count(*) INTO v_count
  FROM banking.v_hold_balance_drift
  WHERE p_tenant_id IS NULL OR tenant_id = p_tenant_id;

  IF v_count > 0 THEN
    RAISE EXCEPTION 'hold balance drift rows found: %', v_count USING ERRCODE = '23514';
  END IF;
END;
$$;

COMMENT ON VIEW banking.v_transaction_balance_violations IS 'Any rows here are severe: the immutable ledger has an unbalanced transaction.';
COMMENT ON VIEW banking.v_account_balance_drift IS 'Any rows here mean cached account balances do not match immutable ledger entries.';
COMMENT ON VIEW banking.v_hold_balance_drift IS 'Any rows here mean cached held balances do not match active holds.';
