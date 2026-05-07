-- V002 reference data.

INSERT INTO banking.currency(currency_code, numeric_code, exponent, currency_name) VALUES
  ('USD', '840', 2, 'US Dollar'),
  ('EUR', '978', 2, 'Euro'),
  ('GBP', '826', 2, 'Pound Sterling'),
  ('INR', '356', 2, 'Indian Rupee'),
  ('MUR', '480', 2, 'Mauritian Rupee')
ON CONFLICT (currency_code) DO UPDATE SET
  numeric_code = EXCLUDED.numeric_code,
  exponent = EXCLUDED.exponent,
  currency_name = EXCLUDED.currency_name,
  enabled = true;

INSERT INTO banking.account_category(account_category_code, normal_balance, description) VALUES
  ('ASSET',     'DEBIT',  'Cash, settlement, receivables, treasury assets'),
  ('LIABILITY', 'CREDIT', 'Customer deposits, payables, borrowed funds'),
  ('EQUITY',    'CREDIT', 'Capital and retained earnings'),
  ('INCOME',    'CREDIT', 'Fees, interest income, gains'),
  ('EXPENSE',   'DEBIT',  'Operating expenses, interest expense, losses')
ON CONFLICT (account_category_code) DO UPDATE SET
  normal_balance = EXCLUDED.normal_balance,
  description = EXCLUDED.description;

INSERT INTO banking.transaction_type(transaction_type_code, description, enabled, requires_external_ref) VALUES
  ('CUSTOMER_DEPOSIT',       'Customer deposit funded by settlement account', true, false),
  ('CUSTOMER_WITHDRAWAL',    'Customer withdrawal paid from settlement account', true, false),
  ('INTERNAL_TRANSFER',      'Transfer between accounts in the same tenant ledger', true, false),
  ('FEE_CHARGE',             'Fee charged to customer account', true, false),
  ('INTEREST_ACCRUAL',       'Interest accrual or capitalization', true, false),
  ('REVERSAL',               'Reversal of a previously posted transaction', true, false),
  ('ADJUSTMENT',             'Controlled back-office adjustment', true, true)
ON CONFLICT (transaction_type_code) DO UPDATE SET
  description = EXCLUDED.description,
  enabled = EXCLUDED.enabled,
  requires_external_ref = EXCLUDED.requires_external_ref;
