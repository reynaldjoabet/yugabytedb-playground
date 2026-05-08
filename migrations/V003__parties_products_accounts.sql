-- V003 parties, GL chart of accounts, account products, accounts, and balances.

CREATE TABLE banking.party (
  tenant_id          uuid NOT NULL,
  party_id           uuid NOT NULL DEFAULT gen_random_uuid(),
  party_type         text NOT NULL CHECK (party_type IN ('PERSON', 'ORGANIZATION')),
  display_name       text NOT NULL CHECK (length(trim(display_name)) > 0),
  external_party_ref text,
  kyc_status         text NOT NULL DEFAULT 'PENDING'
                     CHECK (kyc_status IN ('PENDING', 'VERIFIED', 'REJECTED', 'EXPIRED')),
  risk_rating        text NOT NULL DEFAULT 'LOW'
                     CHECK (risk_rating IN ('LOW', 'MEDIUM', 'HIGH', 'PROHIBITED')),
  tax_id_hash        bytea,
  pii_vault_ref      text,
  metadata           jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at         timestamptz NOT NULL DEFAULT clock_timestamp(),
  updated_at         timestamptz NOT NULL DEFAULT clock_timestamp(),
  PRIMARY KEY ((tenant_id, party_id) HASH),
  CONSTRAINT fk_party_tenant FOREIGN KEY (tenant_id) REFERENCES banking.tenant(tenant_id),
  CONSTRAINT ux_party_external_ref UNIQUE (tenant_id, external_party_ref)
) WITH (COLOCATION = false) SPLIT INTO 16 TABLETS;

-- General-ledger chart of accounts. Hierarchical: rollup nodes (is_postable=false)
-- group postable leaves. Every customer-facing account FKs a postable leaf.
CREATE TABLE banking.gl_account (
  tenant_id              uuid NOT NULL,
  gl_account_id          uuid NOT NULL DEFAULT gen_random_uuid(),
  gl_code                text NOT NULL CHECK (gl_code ~ '^[A-Z0-9_:.-]{1,32}$'),
  gl_name                text NOT NULL CHECK (length(trim(gl_name)) > 0),
  account_category_code  text NOT NULL,
  normal_balance         text NOT NULL CHECK (normal_balance IN ('DEBIT', 'CREDIT')),
  parent_gl_account_id   uuid,
  is_postable            boolean NOT NULL DEFAULT true,
  is_active              boolean NOT NULL DEFAULT true,
  metadata               jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at             timestamptz NOT NULL DEFAULT clock_timestamp(),
  updated_at             timestamptz NOT NULL DEFAULT clock_timestamp(),
  PRIMARY KEY ((tenant_id, gl_account_id) HASH),
  CONSTRAINT fk_gl_account_tenant FOREIGN KEY (tenant_id) REFERENCES banking.tenant(tenant_id),
  CONSTRAINT fk_gl_account_category FOREIGN KEY (account_category_code) REFERENCES banking.account_category(account_category_code),
  CONSTRAINT fk_gl_account_parent FOREIGN KEY (tenant_id, parent_gl_account_id) REFERENCES banking.gl_account(tenant_id, gl_account_id),
  CONSTRAINT ux_gl_account_code UNIQUE (tenant_id, gl_code),
  CONSTRAINT ck_gl_account_self_parent CHECK (parent_gl_account_id IS NULL OR parent_gl_account_id <> gl_account_id),
  -- Accounting identity: assets/expenses are debit-normal; liabilities/equity/income are credit-normal.
  CONSTRAINT ck_gl_account_identity CHECK (
       (account_category_code IN ('ASSET','EXPENSE')             AND normal_balance = 'DEBIT')
    OR (account_category_code IN ('LIABILITY','EQUITY','INCOME') AND normal_balance = 'CREDIT')
  )
) WITH (COLOCATION = false) SPLIT INTO 8 TABLETS;

CREATE TABLE banking.account_product (
  tenant_id              uuid NOT NULL,
  product_code           text NOT NULL CHECK (product_code ~ '^[A-Z0-9_:-]{2,64}$'),
  product_name           text NOT NULL CHECK (length(trim(product_name)) > 0),
  account_category_code  text NOT NULL,
  default_currency_code  banking.currency_code,
  default_gl_account_id  uuid,
  allow_negative_default boolean NOT NULL DEFAULT false,
  overdraft_limit_default_minor numeric(38,0) NOT NULL DEFAULT 0 CHECK (overdraft_limit_default_minor >= 0),
  status                 text NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'DISABLED', 'RETIRED')),
  metadata               jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at             timestamptz NOT NULL DEFAULT clock_timestamp(),
  updated_at             timestamptz NOT NULL DEFAULT clock_timestamp(),
  PRIMARY KEY ((tenant_id, product_code) HASH),
  CONSTRAINT fk_account_product_tenant FOREIGN KEY (tenant_id) REFERENCES banking.tenant(tenant_id),
  CONSTRAINT fk_account_product_category FOREIGN KEY (account_category_code) REFERENCES banking.account_category(account_category_code),
  CONSTRAINT fk_account_product_currency FOREIGN KEY (default_currency_code) REFERENCES banking.currency(currency_code),
  CONSTRAINT fk_account_product_gl FOREIGN KEY (tenant_id, default_gl_account_id) REFERENCES banking.gl_account(tenant_id, gl_account_id)
);

CREATE TABLE banking.account (
  tenant_id                 uuid NOT NULL,
  account_id                uuid NOT NULL DEFAULT gen_random_uuid(),
  account_no                text NOT NULL CHECK (account_no ~ '^[A-Z0-9][A-Z0-9:-]{4,63}$'),
  party_id                  uuid,
  product_code              text NOT NULL,
  gl_account_id             uuid NOT NULL,
  account_category_code     text NOT NULL,
  normal_balance            text NOT NULL CHECK (normal_balance IN ('DEBIT', 'CREDIT')),
  currency_code             banking.currency_code NOT NULL,
  status                    text NOT NULL DEFAULT 'PENDING'
                            CHECK (status IN ('PENDING', 'OPEN', 'FROZEN', 'DORMANT', 'CLOSED')),
  allow_debits              boolean NOT NULL DEFAULT true,
  allow_credits             boolean NOT NULL DEFAULT true,
  allow_negative_balance    boolean NOT NULL DEFAULT false,
  overdraft_limit_minor     numeric(38,0) NOT NULL DEFAULT 0 CHECK (overdraft_limit_minor >= 0),
  opened_at                 timestamptz,
  closed_at                 timestamptz,
  closure_reason            text,
  metadata                  jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at                timestamptz NOT NULL DEFAULT clock_timestamp(),
  updated_at                timestamptz NOT NULL DEFAULT clock_timestamp(),
  PRIMARY KEY ((tenant_id, account_id) HASH),
  CONSTRAINT fk_account_tenant FOREIGN KEY (tenant_id) REFERENCES banking.tenant(tenant_id),
  CONSTRAINT fk_account_party FOREIGN KEY (tenant_id, party_id) REFERENCES banking.party(tenant_id, party_id),
  CONSTRAINT fk_account_product FOREIGN KEY (tenant_id, product_code) REFERENCES banking.account_product(tenant_id, product_code),
  CONSTRAINT fk_account_gl FOREIGN KEY (tenant_id, gl_account_id) REFERENCES banking.gl_account(tenant_id, gl_account_id),
  CONSTRAINT fk_account_category FOREIGN KEY (account_category_code) REFERENCES banking.account_category(account_category_code),
  CONSTRAINT fk_account_currency FOREIGN KEY (currency_code) REFERENCES banking.currency(currency_code),
  CONSTRAINT ux_account_no UNIQUE (tenant_id, account_no),
  CONSTRAINT ck_account_closed_state CHECK (
    (status <> 'CLOSED' AND closed_at IS NULL) OR
    (status = 'CLOSED' AND closed_at IS NOT NULL)
  )
) WITH (COLOCATION = false) SPLIT INTO 32 TABLETS;

CREATE TABLE banking.account_balance (
  tenant_id             uuid NOT NULL,
  account_id            uuid NOT NULL,
  currency_code         banking.currency_code NOT NULL,
  ledger_balance_minor  numeric(38,0) NOT NULL DEFAULT 0,
  held_balance_minor    numeric(38,0) NOT NULL DEFAULT 0 CHECK (held_balance_minor >= 0),
  available_balance_minor numeric(38,0) GENERATED ALWAYS AS (ledger_balance_minor - held_balance_minor) STORED,
  version               bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
  last_transaction_id   uuid,
  updated_at            timestamptz NOT NULL DEFAULT clock_timestamp(),
  PRIMARY KEY ((tenant_id, account_id) HASH),
  CONSTRAINT fk_account_balance_account FOREIGN KEY (tenant_id, account_id) REFERENCES banking.account(tenant_id, account_id),
  CONSTRAINT fk_account_balance_currency FOREIGN KEY (currency_code) REFERENCES banking.currency(currency_code)
) WITH (COLOCATION = false) SPLIT INTO 32 TABLETS;

CREATE INDEX idx_party_external_ref ON banking.party(external_party_ref HASH, tenant_id ASC) SPLIT INTO 16 TABLETS;
CREATE INDEX idx_party_kyc_status ON banking.party(tenant_id HASH, kyc_status ASC) SPLIT INTO 8 TABLETS;
CREATE INDEX idx_gl_account_parent ON banking.gl_account(tenant_id HASH, parent_gl_account_id ASC) SPLIT INTO 4 TABLETS WHERE parent_gl_account_id IS NOT NULL;
CREATE INDEX idx_gl_account_category ON banking.gl_account(tenant_id HASH, account_category_code ASC, gl_code ASC) SPLIT INTO 4 TABLETS;
CREATE INDEX idx_account_party ON banking.account(party_id HASH, account_id ASC) SPLIT INTO 16 TABLETS;
CREATE INDEX idx_account_status ON banking.account(tenant_id HASH, status ASC, account_no ASC) SPLIT INTO 16 TABLETS;
CREATE INDEX idx_account_product ON banking.account(tenant_id HASH, product_code ASC) SPLIT INTO 8 TABLETS;
CREATE INDEX idx_account_gl ON banking.account(tenant_id HASH, gl_account_id ASC) SPLIT INTO 8 TABLETS;

CREATE OR REPLACE FUNCTION banking.assert_gl_account_consistency()
RETURNS trigger
LANGUAGE plpgsql
SET search_path = pg_catalog, banking
AS $$
DECLARE
  v_parent_category text;
  v_parent_active boolean;
BEGIN
  IF NEW.parent_gl_account_id IS NOT NULL THEN
    SELECT g.account_category_code, g.is_active
      INTO v_parent_category, v_parent_active
    FROM banking.gl_account g
    WHERE g.tenant_id = NEW.tenant_id
      AND g.gl_account_id = NEW.parent_gl_account_id;

    IF v_parent_category IS NULL THEN
      RAISE EXCEPTION 'parent gl_account % does not exist for tenant %', NEW.parent_gl_account_id, NEW.tenant_id USING ERRCODE = '23503';
    END IF;

    IF v_parent_category <> NEW.account_category_code THEN
      RAISE EXCEPTION 'parent gl_account % category % does not match child category %',
        NEW.parent_gl_account_id, v_parent_category, NEW.account_category_code USING ERRCODE = '23514';
    END IF;

    IF NOT v_parent_active THEN
      RAISE EXCEPTION 'parent gl_account % is inactive', NEW.parent_gl_account_id USING ERRCODE = '23514';
    END IF;
  END IF;

  RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION banking.assert_account_consistency()
RETURNS trigger
LANGUAGE plpgsql
SET search_path = pg_catalog, banking
AS $$
DECLARE
  v_cat_normal text;
  v_product_status text;
  v_product_category text;
  v_product_currency banking.currency_code;
  v_gl_postable boolean;
  v_gl_active boolean;
  v_gl_normal text;
  v_gl_category text;
BEGIN
  SELECT c.normal_balance
    INTO v_cat_normal
  FROM banking.account_category c
  WHERE c.account_category_code = NEW.account_category_code;

  IF v_cat_normal IS NULL THEN
    RAISE EXCEPTION 'unknown account_category %', NEW.account_category_code USING ERRCODE = '23503';
  END IF;

  IF NEW.normal_balance <> v_cat_normal THEN
    RAISE EXCEPTION 'normal_balance % does not match category % normal balance %',
      NEW.normal_balance, NEW.account_category_code, v_cat_normal USING ERRCODE = '23514';
  END IF;

  SELECT p.status, p.account_category_code, p.default_currency_code
    INTO v_product_status, v_product_category, v_product_currency
  FROM banking.account_product p
  WHERE p.tenant_id = NEW.tenant_id
    AND p.product_code = NEW.product_code;

  IF v_product_status IS NULL THEN
    RAISE EXCEPTION 'unknown product % for tenant %', NEW.product_code, NEW.tenant_id USING ERRCODE = '23503';
  END IF;

  IF v_product_status <> 'ACTIVE' THEN
    RAISE EXCEPTION 'account product % is not active', NEW.product_code USING ERRCODE = '23514';
  END IF;

  IF v_product_category <> NEW.account_category_code THEN
    RAISE EXCEPTION 'product % category % does not match account category %',
      NEW.product_code, v_product_category, NEW.account_category_code USING ERRCODE = '23514';
  END IF;

  IF v_product_currency IS NOT NULL AND v_product_currency <> NEW.currency_code THEN
    RAISE EXCEPTION 'product % default currency % does not match account currency %',
      NEW.product_code, v_product_currency, NEW.currency_code USING ERRCODE = '23514';
  END IF;

  SELECT g.is_postable, g.is_active, g.normal_balance, g.account_category_code
    INTO v_gl_postable, v_gl_active, v_gl_normal, v_gl_category
  FROM banking.gl_account g
  WHERE g.tenant_id = NEW.tenant_id
    AND g.gl_account_id = NEW.gl_account_id;

  IF v_gl_postable IS NULL THEN
    RAISE EXCEPTION 'unknown gl_account % for tenant %', NEW.gl_account_id, NEW.tenant_id USING ERRCODE = '23503';
  END IF;

  IF NOT v_gl_postable THEN
    RAISE EXCEPTION 'gl_account % is a rollup node (is_postable=false)', NEW.gl_account_id USING ERRCODE = '23514';
  END IF;

  IF NOT v_gl_active THEN
    RAISE EXCEPTION 'gl_account % is inactive', NEW.gl_account_id USING ERRCODE = '23514';
  END IF;

  IF v_gl_normal <> NEW.normal_balance THEN
    RAISE EXCEPTION 'gl_account % normal_balance % does not match account normal_balance %',
      NEW.gl_account_id, v_gl_normal, NEW.normal_balance USING ERRCODE = '23514';
  END IF;

  IF v_gl_category <> NEW.account_category_code THEN
    RAISE EXCEPTION 'gl_account % category % does not match account category %',
      NEW.gl_account_id, v_gl_category, NEW.account_category_code USING ERRCODE = '23514';
  END IF;

  IF NEW.status = 'OPEN' AND NEW.opened_at IS NULL THEN
    NEW.opened_at := clock_timestamp();
  END IF;

  RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION banking.create_account_balance_row()
RETURNS trigger
LANGUAGE plpgsql
SET search_path = pg_catalog, banking
AS $$
BEGIN
  INSERT INTO banking.account_balance(tenant_id, account_id, currency_code)
  VALUES (NEW.tenant_id, NEW.account_id, NEW.currency_code);
  RETURN NEW;
END;
$$;

CREATE TRIGGER zz_party_touch_updated_at
BEFORE UPDATE ON banking.party
FOR EACH ROW
EXECUTE FUNCTION banking.touch_updated_at();

CREATE TRIGGER zz_gl_account_touch_updated_at
BEFORE UPDATE ON banking.gl_account
FOR EACH ROW
EXECUTE FUNCTION banking.touch_updated_at();

CREATE TRIGGER zz_gl_account_assert_consistency
BEFORE INSERT OR UPDATE OF parent_gl_account_id, account_category_code, normal_balance, is_active ON banking.gl_account
FOR EACH ROW
EXECUTE FUNCTION banking.assert_gl_account_consistency();

CREATE TRIGGER zz_account_product_touch_updated_at
BEFORE UPDATE ON banking.account_product
FOR EACH ROW
EXECUTE FUNCTION banking.touch_updated_at();

CREATE TRIGGER zz_account_assert_consistency
BEFORE INSERT OR UPDATE OF product_code, gl_account_id, account_category_code, normal_balance, currency_code, status, opened_at ON banking.account
FOR EACH ROW
EXECUTE FUNCTION banking.assert_account_consistency();

CREATE TRIGGER zz_account_touch_updated_at
BEFORE UPDATE ON banking.account
FOR EACH ROW
EXECUTE FUNCTION banking.touch_updated_at();

CREATE TRIGGER zz_account_create_balance
AFTER INSERT ON banking.account
FOR EACH ROW
EXECUTE FUNCTION banking.create_account_balance_row();

CREATE TRIGGER audit_tenant_change
AFTER INSERT OR UPDATE OR DELETE ON banking.tenant
FOR EACH ROW
EXECUTE FUNCTION audit.capture_row_change();

CREATE TRIGGER audit_party_change
AFTER INSERT OR UPDATE OR DELETE ON banking.party
FOR EACH ROW
EXECUTE FUNCTION audit.capture_row_change();

CREATE TRIGGER audit_gl_account_change
AFTER INSERT OR UPDATE OR DELETE ON banking.gl_account
FOR EACH ROW
EXECUTE FUNCTION audit.capture_row_change();

CREATE TRIGGER audit_account_product_change
AFTER INSERT OR UPDATE OR DELETE ON banking.account_product
FOR EACH ROW
EXECUTE FUNCTION audit.capture_row_change();

CREATE TRIGGER audit_account_change
AFTER INSERT OR UPDATE OR DELETE ON banking.account
FOR EACH ROW
EXECUTE FUNCTION audit.capture_row_change();

COMMENT ON TABLE banking.gl_account IS 'Per-tenant chart of accounts. Hierarchical via parent_gl_account_id; only postable leaves can back a banking.account.';
COMMENT ON TABLE banking.account IS 'Customer and operational accounts. Every row maps to a postable banking.gl_account leaf. normal_balance, account_category_code, and currency_code are denormalized for posting performance and validated by trigger against the GL.';
COMMENT ON TABLE banking.account_balance IS 'Mutable balance cache derived from immutable ledger entries plus active funds holds.';
