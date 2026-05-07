-- V003 parties, account products, accounts, and balances.

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

CREATE TABLE banking.account_product (
  tenant_id              uuid NOT NULL,
  product_code           text NOT NULL CHECK (product_code ~ '^[A-Z0-9_:-]{2,64}$'),
  product_name           text NOT NULL CHECK (length(trim(product_name)) > 0),
  account_category_code  text NOT NULL,
  default_currency_code  banking.currency_code,
  allow_negative_default boolean NOT NULL DEFAULT false,
  overdraft_limit_default_minor numeric(38,0) NOT NULL DEFAULT 0 CHECK (overdraft_limit_default_minor >= 0),
  status                 text NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'DISABLED', 'RETIRED')),
  metadata               jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at             timestamptz NOT NULL DEFAULT clock_timestamp(),
  updated_at             timestamptz NOT NULL DEFAULT clock_timestamp(),
  PRIMARY KEY ((tenant_id, product_code) HASH),
  CONSTRAINT fk_account_product_tenant FOREIGN KEY (tenant_id) REFERENCES banking.tenant(tenant_id),
  CONSTRAINT fk_account_product_category FOREIGN KEY (account_category_code) REFERENCES banking.account_category(account_category_code),
  CONSTRAINT fk_account_product_currency FOREIGN KEY (default_currency_code) REFERENCES banking.currency(currency_code)
);

CREATE TABLE banking.account (
  tenant_id                 uuid NOT NULL,
  account_id                uuid NOT NULL DEFAULT gen_random_uuid(),
  account_no                text NOT NULL CHECK (account_no ~ '^[A-Z0-9][A-Z0-9:-]{4,63}$'),
  party_id                  uuid,
  product_code              text NOT NULL,
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
CREATE INDEX idx_account_party ON banking.account(party_id HASH, account_id ASC) SPLIT INTO 16 TABLETS;
CREATE INDEX idx_account_status ON banking.account(tenant_id HASH, status ASC, account_no ASC) SPLIT INTO 16 TABLETS;
CREATE INDEX idx_account_product ON banking.account(tenant_id HASH, product_code ASC) SPLIT INTO 8 TABLETS;

CREATE OR REPLACE FUNCTION banking.assert_account_consistency()
RETURNS trigger
LANGUAGE plpgsql
SET search_path = pg_catalog, banking
AS $$
DECLARE
  v_normal_balance text;
  v_product_status text;
BEGIN
  SELECT c.normal_balance, p.status
    INTO v_normal_balance, v_product_status
  FROM banking.account_category c
  JOIN banking.account_product p
    ON p.account_category_code = c.account_category_code
   AND p.tenant_id = NEW.tenant_id
   AND p.product_code = NEW.product_code
  WHERE c.account_category_code = NEW.account_category_code;

  IF v_normal_balance IS NULL THEN
    RAISE EXCEPTION 'invalid account product/category for account %', NEW.account_no USING ERRCODE = '23503';
  END IF;

  IF v_product_status <> 'ACTIVE' THEN
    RAISE EXCEPTION 'account product % is not active', NEW.product_code USING ERRCODE = '23514';
  END IF;

  IF NEW.normal_balance <> v_normal_balance THEN
    RAISE EXCEPTION 'normal_balance % does not match category % normal balance %',
      NEW.normal_balance, NEW.account_category_code, v_normal_balance USING ERRCODE = '23514';
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

CREATE TRIGGER zz_account_product_touch_updated_at
BEFORE UPDATE ON banking.account_product
FOR EACH ROW
EXECUTE FUNCTION banking.touch_updated_at();

CREATE TRIGGER zz_account_assert_consistency
BEFORE INSERT OR UPDATE OF product_code, account_category_code, normal_balance, status, opened_at ON banking.account
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

CREATE TRIGGER audit_party_change
AFTER INSERT OR UPDATE OR DELETE ON banking.party
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

COMMENT ON TABLE banking.account IS 'Customer and GL accounts. normal_balance determines whether debit or credit increases the displayed balance.';
COMMENT ON TABLE banking.account_balance IS 'Mutable balance cache derived from immutable ledger entries plus active funds holds.';
