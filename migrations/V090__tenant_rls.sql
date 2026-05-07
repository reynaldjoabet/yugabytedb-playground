-- Optional tenant row-level security.
-- Use only when every runtime transaction sets app.tenant_id, for example:
--   BEGIN;
--   SET LOCAL app.tenant_id = '...tenant uuid...';
--   ...queries...
--   COMMIT;

CREATE OR REPLACE FUNCTION banking.current_tenant_id()
RETURNS uuid
LANGUAGE sql
STABLE
AS $$
  SELECT nullif(current_setting('app.tenant_id', true), '')::uuid;
$$;

ALTER TABLE banking.party ENABLE ROW LEVEL SECURITY;
ALTER TABLE banking.account_product ENABLE ROW LEVEL SECURITY;
ALTER TABLE banking.account ENABLE ROW LEVEL SECURITY;
ALTER TABLE banking.account_balance ENABLE ROW LEVEL SECURITY;
ALTER TABLE banking.posting_idempotency ENABLE ROW LEVEL SECURITY;
ALTER TABLE banking.ledger_transaction ENABLE ROW LEVEL SECURITY;
ALTER TABLE banking.ledger_entry ENABLE ROW LEVEL SECURITY;
ALTER TABLE banking.ledger_reversal ENABLE ROW LEVEL SECURITY;
ALTER TABLE banking.outbox_event ENABLE ROW LEVEL SECURITY;
ALTER TABLE banking.funds_hold ENABLE ROW LEVEL SECURITY;

CREATE POLICY party_tenant_isolation ON banking.party
  USING (tenant_id = banking.current_tenant_id())
  WITH CHECK (tenant_id = banking.current_tenant_id());

CREATE POLICY account_product_tenant_isolation ON banking.account_product
  USING (tenant_id = banking.current_tenant_id())
  WITH CHECK (tenant_id = banking.current_tenant_id());

CREATE POLICY account_tenant_isolation ON banking.account
  USING (tenant_id = banking.current_tenant_id())
  WITH CHECK (tenant_id = banking.current_tenant_id());

CREATE POLICY account_balance_tenant_isolation ON banking.account_balance
  USING (tenant_id = banking.current_tenant_id())
  WITH CHECK (tenant_id = banking.current_tenant_id());

CREATE POLICY posting_idempotency_tenant_isolation ON banking.posting_idempotency
  USING (tenant_id = banking.current_tenant_id())
  WITH CHECK (tenant_id = banking.current_tenant_id());

CREATE POLICY ledger_transaction_tenant_isolation ON banking.ledger_transaction
  USING (tenant_id = banking.current_tenant_id())
  WITH CHECK (tenant_id = banking.current_tenant_id());

CREATE POLICY ledger_entry_tenant_isolation ON banking.ledger_entry
  USING (tenant_id = banking.current_tenant_id())
  WITH CHECK (tenant_id = banking.current_tenant_id());

CREATE POLICY ledger_reversal_tenant_isolation ON banking.ledger_reversal
  USING (tenant_id = banking.current_tenant_id())
  WITH CHECK (tenant_id = banking.current_tenant_id());

CREATE POLICY outbox_event_tenant_isolation ON banking.outbox_event
  USING (tenant_id = banking.current_tenant_id())
  WITH CHECK (tenant_id = banking.current_tenant_id());

CREATE POLICY funds_hold_tenant_isolation ON banking.funds_hold
  USING (tenant_id = banking.current_tenant_id())
  WITH CHECK (tenant_id = banking.current_tenant_id());

GRANT EXECUTE ON FUNCTION banking.current_tenant_id() TO corebank_app, corebank_readonly, corebank_outbox, corebank_ops;
