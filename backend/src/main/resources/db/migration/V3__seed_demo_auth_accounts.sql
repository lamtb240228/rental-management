DO $$
DECLARE
    admin_user_id BIGINT;
    tenant_user_id BIGINT;
BEGIN
    INSERT INTO user_accounts(email, password_hash, full_name, phone, status, email_verified)
    VALUES (
        'admin@rental.local',
        '$2a$10$XTuJEn5mMogncR9ofKco7OPWXnJHIx/W7MciYTrSrCAq84p6lewWu',
        'Demo Admin',
        '0900000001',
        'ACTIVE',
        TRUE
    )
    ON CONFLICT (email) DO UPDATE
    SET password_hash = EXCLUDED.password_hash,
        full_name = EXCLUDED.full_name,
        phone = EXCLUDED.phone,
        status = EXCLUDED.status,
        email_verified = EXCLUDED.email_verified,
        updated_at = CURRENT_TIMESTAMP
    RETURNING id INTO admin_user_id;

    INSERT INTO user_roles(user_id, role_id)
    SELECT admin_user_id, id
    FROM roles
    WHERE name = 'ADMIN'
    ON CONFLICT DO NOTHING;

    INSERT INTO user_accounts(email, password_hash, full_name, phone, status, email_verified)
    VALUES (
        'tenant@rental.local',
        '$2a$10$XTuJEn5mMogncR9ofKco7OPWXnJHIx/W7MciYTrSrCAq84p6lewWu',
        'Demo Tenant',
        '0911111111',
        'ACTIVE',
        TRUE
    )
    ON CONFLICT (email) DO UPDATE
    SET password_hash = EXCLUDED.password_hash,
        full_name = EXCLUDED.full_name,
        phone = EXCLUDED.phone,
        status = EXCLUDED.status,
        email_verified = EXCLUDED.email_verified,
        updated_at = CURRENT_TIMESTAMP
    RETURNING id INTO tenant_user_id;

    INSERT INTO user_roles(user_id, role_id)
    SELECT tenant_user_id, id
    FROM roles
    WHERE name = 'TENANT'
    ON CONFLICT DO NOTHING;

    UPDATE tenants
    SET user_account_id = tenant_user_id,
        email = 'tenant@rental.local',
        phone = '0911111111',
        updated_at = CURRENT_TIMESTAMP
    WHERE identity_number = 'DEMO123456'
      AND deleted_at IS NULL;
END $$;
