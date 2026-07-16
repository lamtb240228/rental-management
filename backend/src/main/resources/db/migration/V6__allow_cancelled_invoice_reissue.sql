DROP INDEX IF EXISTS uk_invoices_contract_period_active;

CREATE UNIQUE INDEX uk_invoices_contract_period_active
    ON invoices(contract_id, billing_year, billing_month)
    WHERE deleted_at IS NULL AND status <> 'CANCELLED';
