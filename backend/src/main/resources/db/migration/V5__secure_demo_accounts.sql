UPDATE user_accounts
SET status = 'LOCKED',
    updated_at = CURRENT_TIMESTAMP
WHERE lower(email) IN (
    'demo@rental.local',
    'admin@rental.local',
    'tenant@rental.local'
)
  AND deleted_at IS NULL;
