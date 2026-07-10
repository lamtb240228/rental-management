INSERT INTO user_accounts (
    email,
    password_hash,
    full_name,
    phone,
    status,
    email_verified
)
VALUES (
    'admin@rental.local',
    'DEV_ONLY_NOT_A_REAL_PASSWORD_HASH',
    'System Administrator',
    '0900000000',
    'ACTIVE',
    TRUE
);

INSERT INTO user_roles (
    user_id,
    role_id
)
SELECT
    user_account.id,
    role.id
FROM user_accounts AS user_account
JOIN roles AS role
    ON role.name = 'ADMIN'
WHERE user_account.email = 'admin@rental.local';