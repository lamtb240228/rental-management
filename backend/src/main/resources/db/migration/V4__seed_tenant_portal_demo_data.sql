DO $$
DECLARE
    tenant_user_id BIGINT;
    demo_tenant_id BIGINT;
    room_101_id BIGINT;
BEGIN
    SELECT id INTO tenant_user_id
    FROM user_accounts
    WHERE email = 'tenant@rental.local'
      AND deleted_at IS NULL
    LIMIT 1;

    SELECT id INTO demo_tenant_id
    FROM tenants
    WHERE user_account_id = tenant_user_id
      AND identity_number = 'DEMO123456'
      AND deleted_at IS NULL
    LIMIT 1;

    SELECT r.id INTO room_101_id
    FROM rooms r
    JOIN properties p ON p.id = r.property_id
    WHERE p.name = 'Demo Property'
      AND r.room_number = '101'
      AND r.deleted_at IS NULL
      AND p.deleted_at IS NULL
    LIMIT 1;

    IF tenant_user_id IS NOT NULL
       AND demo_tenant_id IS NOT NULL
       AND room_101_id IS NOT NULL
       AND NOT EXISTS (
           SELECT 1
           FROM maintenance_requests
           WHERE tenant_id = demo_tenant_id
             AND title = 'Tenant portal demo request'
             AND deleted_at IS NULL
       ) THEN
        INSERT INTO maintenance_requests(
            room_id,
            tenant_id,
            created_by,
            title,
            description,
            priority,
            status,
            submitted_at
        )
        VALUES (
            room_101_id,
            demo_tenant_id,
            tenant_user_id,
            'Tenant portal demo request',
            'Water faucet is leaking and needs inspection',
            'MEDIUM',
            'PENDING',
            CURRENT_TIMESTAMP
        );
    END IF;
END $$;
