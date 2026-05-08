-- Tenant row-level security.
-- Use only when every runtime transaction sets app.tenant_id, for example via
-- banking.set_session_context(...) at the start of each request:
--   BEGIN;
--   SELECT banking.set_session_context('user@example', 'req-abc', '...tenant uuid...');
--   ...queries...
--   COMMIT;
--
-- corebank_reconciler holds BYPASSRLS (see V008) so cross-tenant integrity jobs
-- can read every tenant. The migration owner also bypasses RLS during deploys.

-- banking.current_tenant_id() is defined in V001 (foundation) and re-asserted here
-- to keep this module self-contained for review.
CREATE OR REPLACE FUNCTION banking.current_tenant_id()
RETURNS uuid
LANGUAGE sql
STABLE
AS $$
  SELECT nullif(current_setting('app.tenant_id', true), '')::uuid;
$$;

-- ENABLE + FORCE so even the table owner is subject to policies.
ALTER TABLE banking.tenant              ENABLE ROW LEVEL SECURITY;
ALTER TABLE banking.tenant              FORCE  ROW LEVEL SECURITY;
ALTER TABLE banking.party               ENABLE ROW LEVEL SECURITY;
ALTER TABLE banking.party               FORCE  ROW LEVEL SECURITY;
ALTER TABLE banking.gl_account          ENABLE ROW LEVEL SECURITY;
ALTER TABLE banking.gl_account          FORCE  ROW LEVEL SECURITY;
ALTER TABLE banking.account_product     ENABLE ROW LEVEL SECURITY;
ALTER TABLE banking.account_product     FORCE  ROW LEVEL SECURITY;
ALTER TABLE banking.account             ENABLE ROW LEVEL SECURITY;
ALTER TABLE banking.account             FORCE  ROW LEVEL SECURITY;
ALTER TABLE banking.account_balance     ENABLE ROW LEVEL SECURITY;
ALTER TABLE banking.account_balance     FORCE  ROW LEVEL SECURITY;
ALTER TABLE banking.posting_idempotency ENABLE ROW LEVEL SECURITY;
ALTER TABLE banking.posting_idempotency FORCE  ROW LEVEL SECURITY;
ALTER TABLE banking.ledger_transaction  ENABLE ROW LEVEL SECURITY;
ALTER TABLE banking.ledger_transaction  FORCE  ROW LEVEL SECURITY;
ALTER TABLE banking.ledger_entry        ENABLE ROW LEVEL SECURITY;
ALTER TABLE banking.ledger_entry        FORCE  ROW LEVEL SECURITY;
ALTER TABLE banking.ledger_reversal     ENABLE ROW LEVEL SECURITY;
ALTER TABLE banking.ledger_reversal     FORCE  ROW LEVEL SECURITY;
ALTER TABLE banking.outbox_event        ENABLE ROW LEVEL SECURITY;
ALTER TABLE banking.outbox_event        FORCE  ROW LEVEL SECURITY;
ALTER TABLE banking.funds_hold          ENABLE ROW LEVEL SECURITY;
ALTER TABLE banking.funds_hold          FORCE  ROW LEVEL SECURITY;
ALTER TABLE audit.audit_event           ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit.audit_event           FORCE  ROW LEVEL SECURITY;

CREATE POLICY tenant_self_isolation ON banking.tenant
  USING (tenant_id = banking.current_tenant_id())
  WITH CHECK (tenant_id = banking.current_tenant_id());

CREATE POLICY party_tenant_isolation ON banking.party
  USING (tenant_id = banking.current_tenant_id())
  WITH CHECK (tenant_id = banking.current_tenant_id());

CREATE POLICY gl_account_tenant_isolation ON banking.gl_account
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

-- audit_event: trigger inserts may carry NULL tenant_id (e.g. global reference tables).
-- Allow tenant-matching reads; allow NULL-tenant rows to be inserted by the trigger.
CREATE POLICY audit_event_tenant_isolation ON audit.audit_event
  USING (tenant_id = banking.current_tenant_id() OR tenant_id IS NULL)
  WITH CHECK (tenant_id = banking.current_tenant_id() OR tenant_id IS NULL);

GRANT EXECUTE ON FUNCTION banking.current_tenant_id() TO corebank_app, corebank_readonly, corebank_outbox, corebank_ops, corebank_reconciler;
