-- V008 runtime roles and least-privilege grants.
-- Run as a database owner or admin. Replace LOGIN roles with your IAM/secret-management approach.

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'corebank_app') THEN
    CREATE ROLE corebank_app LOGIN;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'corebank_readonly') THEN
    CREATE ROLE corebank_readonly LOGIN;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'corebank_outbox') THEN
    CREATE ROLE corebank_outbox LOGIN;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'corebank_ops') THEN
    CREATE ROLE corebank_ops LOGIN;
  END IF;
END;
$$;

-- Enforce the intended transaction mode for normal runtime connections.
ALTER ROLE corebank_app SET default_transaction_isolation = 'serializable';
ALTER ROLE corebank_ops SET default_transaction_isolation = 'serializable';

REVOKE ALL ON SCHEMA banking FROM PUBLIC;
REVOKE ALL ON SCHEMA audit FROM PUBLIC;
REVOKE ALL ON SCHEMA ops FROM PUBLIC;
REVOKE ALL ON ALL TABLES IN SCHEMA banking FROM PUBLIC;
REVOKE ALL ON ALL TABLES IN SCHEMA audit FROM PUBLIC;
REVOKE ALL ON ALL FUNCTIONS IN SCHEMA banking FROM PUBLIC;
REVOKE ALL ON ALL FUNCTIONS IN SCHEMA audit FROM PUBLIC;

GRANT USAGE ON SCHEMA banking TO corebank_app, corebank_readonly, corebank_outbox, corebank_ops;
GRANT USAGE ON SCHEMA audit TO corebank_ops;
GRANT USAGE ON SCHEMA ops TO corebank_ops;

-- Read access.
GRANT SELECT ON ALL TABLES IN SCHEMA banking TO corebank_readonly, corebank_ops;
GRANT SELECT ON ALL TABLES IN SCHEMA audit TO corebank_ops;

-- Application runtime can onboard parties/accounts/products and read operational state.
GRANT SELECT ON banking.tenant TO corebank_app;
GRANT SELECT ON banking.currency TO corebank_app;
GRANT SELECT ON banking.account_category TO corebank_app;
GRANT SELECT ON banking.transaction_type TO corebank_app;
GRANT SELECT, INSERT, UPDATE ON banking.party TO corebank_app;
GRANT SELECT, INSERT, UPDATE ON banking.account_product TO corebank_app;
GRANT SELECT, INSERT, UPDATE ON banking.account TO corebank_app;
GRANT SELECT ON banking.account_balance TO corebank_app;
GRANT SELECT ON banking.ledger_transaction TO corebank_app;
GRANT SELECT ON banking.ledger_entry TO corebank_app;
GRANT SELECT ON banking.ledger_reversal TO corebank_app;
GRANT SELECT ON banking.funds_hold TO corebank_app;
GRANT SELECT ON banking.outbox_event TO corebank_app;

-- No direct mutation grants on balances, ledger facts, idempotency, or outbox.
REVOKE INSERT, UPDATE, DELETE ON banking.account_balance FROM corebank_app;
REVOKE INSERT, UPDATE, DELETE ON banking.ledger_transaction FROM corebank_app;
REVOKE INSERT, UPDATE, DELETE ON banking.ledger_entry FROM corebank_app;
REVOKE INSERT, UPDATE, DELETE ON banking.ledger_reversal FROM corebank_app;
REVOKE INSERT, UPDATE, DELETE ON banking.posting_idempotency FROM corebank_app;
REVOKE INSERT, UPDATE, DELETE ON banking.outbox_event FROM corebank_app;

-- Money movement APIs.
GRANT EXECUTE ON FUNCTION banking.post_ledger_transaction(uuid, text, text, text, jsonb, text, text, timestamptz, jsonb, text) TO corebank_app;
GRANT EXECUTE ON FUNCTION banking.reverse_ledger_transaction(uuid, uuid, text, text, text, jsonb, text) TO corebank_app;
GRANT EXECUTE ON FUNCTION banking.place_funds_hold(uuid, uuid, text, text, numeric, banking.currency_code, timestamptz, text, text, jsonb, text) TO corebank_app;
GRANT EXECUTE ON FUNCTION banking.release_funds_hold(uuid, uuid, text, text) TO corebank_app;

-- Outbox dispatcher role.
GRANT SELECT ON banking.outbox_event TO corebank_outbox;
GRANT EXECUTE ON FUNCTION banking.claim_outbox_events(text, integer, integer) TO corebank_outbox;
GRANT EXECUTE ON FUNCTION banking.mark_outbox_published(uuid, uuid, text) TO corebank_outbox;
GRANT EXECUTE ON FUNCTION banking.mark_outbox_failed(uuid, uuid, text, text) TO corebank_outbox;

-- Ops role.
GRANT EXECUTE ON FUNCTION banking.expire_due_funds_holds(uuid, integer) TO corebank_ops;
GRANT EXECUTE ON FUNCTION banking.assert_reconciliation_clean(uuid) TO corebank_ops;
GRANT EXECUTE ON FUNCTION banking.release_funds_hold(uuid, uuid, text, text) TO corebank_ops;

-- Future objects created by the migration owner.
ALTER DEFAULT PRIVILEGES IN SCHEMA banking GRANT SELECT ON TABLES TO corebank_readonly, corebank_ops;
ALTER DEFAULT PRIVILEGES IN SCHEMA audit GRANT SELECT ON TABLES TO corebank_ops;

-- Set passwords outside migrations, for example:
-- ALTER ROLE corebank_app PASSWORD '<from-secret-manager>';
-- ALTER ROLE corebank_outbox PASSWORD '<from-secret-manager>';
-- ALTER ROLE corebank_readonly PASSWORD '<from-secret-manager>';
-- ALTER ROLE corebank_ops PASSWORD '<from-secret-manager>';
