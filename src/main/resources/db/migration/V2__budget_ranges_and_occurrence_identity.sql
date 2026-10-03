-- Handles both historical month and current date-range schemas, preserving legacy data.
ALTER TABLE budgets ADD COLUMN IF NOT EXISTS start_date date;
ALTER TABLE budgets ADD COLUMN IF NOT EXISTS end_date date;
DO $$ BEGIN
 IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = current_schema()
            AND table_name = 'budgets' AND column_name = 'budget_month') THEN
   UPDATE budgets SET start_date = coalesce(start_date, date_trunc('month', budget_month)::date),
     end_date = coalesce(end_date, (date_trunc('month', budget_month) + interval '1 month - 1 day')::date);
   -- Retained for forensic/rollback reference, unused by the current application.
   ALTER TABLE budgets ALTER COLUMN budget_month DROP NOT NULL;
 END IF;
END $$;
ALTER TABLE budgets ALTER COLUMN start_date SET NOT NULL;
ALTER TABLE budgets ALTER COLUMN end_date SET NOT NULL;
ALTER TABLE budgets ADD CONSTRAINT ck_budget_dates CHECK (start_date <= end_date);
ALTER TABLE budgets ADD CONSTRAINT ck_budget_amount CHECK (amount >= 0.01);
ALTER TABLE transactions ADD COLUMN recurring_id bigint;
ALTER TABLE transactions ADD COLUMN occurrence_date date;
ALTER TABLE transactions ADD CONSTRAINT uk_recurring_occurrence UNIQUE (recurring_id, occurrence_date);
ALTER TABLE transactions ADD CONSTRAINT ck_occurrence_identity CHECK ((recurring_id IS NULL) = (occurrence_date IS NULL));
ALTER TABLE transactions ADD CONSTRAINT ck_transaction_amount CHECK (amount >= 0.01);
ALTER TABLE recurring_transactions ADD COLUMN last_generated_date date;
-- Old generations have no reliable provenance. Conservatively prevent schedule rewinds
-- before the existing cursor; never guess associations from matching amounts/dates.
UPDATE recurring_transactions SET last_generated_date = next_run_date - 1 WHERE next_run_date > start_date;
ALTER TABLE recurring_transactions ADD CONSTRAINT ck_recurring_dates CHECK (end_date IS NULL OR start_date <= end_date);
ALTER TABLE recurring_transactions ADD CONSTRAINT ck_recurring_amount CHECK (amount >= 0.01);
CREATE INDEX ix_transactions_user_date ON transactions(user_id, transaction_date);
CREATE INDEX ix_transactions_category ON transactions(category_id);
CREATE INDEX ix_budgets_user ON budgets(user_id);
CREATE INDEX ix_budgets_category ON budgets(category_id);
CREATE INDEX ix_recurring_user ON recurring_transactions(user_id);
CREATE INDEX ix_recurring_category ON recurring_transactions(category_id);
CREATE INDEX ix_categories_user ON categories(user_id);
CREATE INDEX ix_accounts_user ON accounts(user_id);
