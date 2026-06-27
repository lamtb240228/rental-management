DO $$
DECLARE
    demo_user_id BIGINT;
    demo_property_id BIGINT;
    room_101_id BIGINT;
    room_102_id BIGINT;
    room_201_id BIGINT;
    demo_tenant_id BIGINT;
    demo_contract_id BIGINT;
    demo_invoice_id BIGINT;
BEGIN
    INSERT INTO user_accounts(email, password_hash, full_name, phone, status, email_verified)
    VALUES (
        'demo@rental.local',
        '$2a$10$XTuJEn5mMogncR9ofKco7OPWXnJHIx/W7MciYTrSrCAq84p6lewWu',
        'Demo Landlord',
        '0900000000',
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
    RETURNING id INTO demo_user_id;

    INSERT INTO user_roles(user_id, role_id)
    SELECT demo_user_id, id
    FROM roles
    WHERE name = 'LANDLORD'
    ON CONFLICT DO NOTHING;

    SELECT id INTO demo_property_id
    FROM properties
    WHERE landlord_id = demo_user_id
      AND name = 'Demo Property'
      AND deleted_at IS NULL
    LIMIT 1;

    IF demo_property_id IS NULL THEN
        INSERT INTO properties(
            landlord_id,
            name,
            address_line,
            ward,
            district,
            province_city,
            description,
            status
        )
        VALUES (
            demo_user_id,
            'Demo Property',
            '120 Nguyen Trai',
            'Ward 1',
            'District 1',
            'Ho Chi Minh City',
            'Seed data for local testing',
            'ACTIVE'
        )
        RETURNING id INTO demo_property_id;
    END IF;

    SELECT id INTO room_101_id
    FROM rooms
    WHERE property_id = demo_property_id
      AND lower(room_number) = '101'
      AND deleted_at IS NULL
    LIMIT 1;

    IF room_101_id IS NULL THEN
        INSERT INTO rooms(
            property_id,
            room_number,
            floor_number,
            area,
            monthly_rent,
            default_deposit,
            max_occupants,
            status,
            description
        )
        VALUES (demo_property_id, '101', 1, 22.50, 3000000, 3000000, 2, 'OCCUPIED', 'Demo occupied room')
        RETURNING id INTO room_101_id;
    END IF;

    SELECT id INTO room_102_id
    FROM rooms
    WHERE property_id = demo_property_id
      AND lower(room_number) = '102'
      AND deleted_at IS NULL
    LIMIT 1;

    IF room_102_id IS NULL THEN
        INSERT INTO rooms(
            property_id,
            room_number,
            floor_number,
            area,
            monthly_rent,
            default_deposit,
            max_occupants,
            status,
            description
        )
        VALUES (demo_property_id, '102', 1, 20.00, 2800000, 2800000, 2, 'AVAILABLE', 'Demo available room')
        RETURNING id INTO room_102_id;
    END IF;

    SELECT id INTO room_201_id
    FROM rooms
    WHERE property_id = demo_property_id
      AND lower(room_number) = '201'
      AND deleted_at IS NULL
    LIMIT 1;

    IF room_201_id IS NULL THEN
        INSERT INTO rooms(
            property_id,
            room_number,
            floor_number,
            area,
            monthly_rent,
            default_deposit,
            max_occupants,
            status,
            description
        )
        VALUES (demo_property_id, '201', 2, 24.00, 3300000, 3300000, 3, 'MAINTENANCE', 'Demo maintenance room')
        RETURNING id INTO room_201_id;
    END IF;

    SELECT id INTO demo_tenant_id
    FROM tenants
    WHERE landlord_id = demo_user_id
      AND identity_number = 'DEMO123456'
      AND deleted_at IS NULL
    LIMIT 1;

    IF demo_tenant_id IS NULL THEN
        INSERT INTO tenants(
            landlord_id,
            full_name,
            date_of_birth,
            phone,
            email,
            identity_number,
            permanent_address,
            status
        )
        VALUES (
            demo_user_id,
            'Demo Tenant',
            DATE '1998-05-20',
            '0911111111',
            'tenant@rental.local',
            'DEMO123456',
            'Demo permanent address',
            'ACTIVE'
        )
        RETURNING id INTO demo_tenant_id;
    END IF;

    INSERT INTO rental_contracts(
        room_id,
        contract_code,
        start_date,
        end_date,
        monthly_rent,
        deposit_amount,
        status,
        notes
    )
    VALUES (
        room_101_id,
        'CT-DEMO-001',
        DATE '2026-06-01',
        DATE '2027-05-31',
        3000000,
        3000000,
        'ACTIVE',
        'Demo active contract'
    )
    ON CONFLICT (contract_code) DO UPDATE
    SET room_id = EXCLUDED.room_id,
        start_date = EXCLUDED.start_date,
        end_date = EXCLUDED.end_date,
        monthly_rent = EXCLUDED.monthly_rent,
        deposit_amount = EXCLUDED.deposit_amount,
        status = EXCLUDED.status,
        notes = EXCLUDED.notes,
        updated_at = CURRENT_TIMESTAMP
    RETURNING id INTO demo_contract_id;

    INSERT INTO contract_tenants(contract_id, tenant_id, primary_tenant, move_in_date)
    VALUES (demo_contract_id, demo_tenant_id, TRUE, DATE '2026-06-01')
    ON CONFLICT (contract_id, tenant_id) DO NOTHING;

    IF NOT EXISTS (
        SELECT 1
        FROM utility_readings
        WHERE room_id = room_101_id
          AND billing_year = 2026
          AND billing_month = 6
          AND deleted_at IS NULL
    ) THEN
        INSERT INTO utility_readings(
            room_id,
            billing_year,
            billing_month,
            electricity_old_reading,
            electricity_new_reading,
            electricity_unit_price,
            water_old_reading,
            water_new_reading,
            water_unit_price
        )
        VALUES (
            room_101_id,
            2026,
            6,
            1200,
            1300,
            3500,
            80,
            86,
            20000
        );
    END IF;

    SELECT id INTO demo_invoice_id
    FROM invoices
    WHERE contract_id = demo_contract_id
      AND billing_year = 2026
      AND billing_month = 6
      AND deleted_at IS NULL
    LIMIT 1;

    IF demo_invoice_id IS NULL THEN
        INSERT INTO invoices(
            contract_id,
            invoice_number,
            billing_year,
            billing_month,
            issue_date,
            due_date,
            subtotal,
            discount_amount,
            total_amount,
            paid_amount,
            status,
            notes
        )
        VALUES (
            demo_contract_id,
            'INV-DEMO-202606',
            2026,
            6,
            DATE '2026-06-25',
            DATE '2026-07-05',
            3570000,
            0,
            3570000,
            1000000,
            'PARTIALLY_PAID',
            'Demo invoice'
        )
        RETURNING id INTO demo_invoice_id;
    END IF;

    INSERT INTO invoice_items(invoice_id, item_type, description, quantity, unit_price, amount)
    SELECT demo_invoice_id, 'RENT', 'Monthly rent', 1, 3000000, 3000000
    WHERE NOT EXISTS (
        SELECT 1 FROM invoice_items WHERE invoice_id = demo_invoice_id AND item_type = 'RENT'
    );

    INSERT INTO invoice_items(invoice_id, item_type, description, quantity, unit_price, amount)
    SELECT demo_invoice_id, 'ELECTRICITY', 'Electricity usage', 100, 3500, 350000
    WHERE NOT EXISTS (
        SELECT 1 FROM invoice_items WHERE invoice_id = demo_invoice_id AND item_type = 'ELECTRICITY'
    );

    INSERT INTO invoice_items(invoice_id, item_type, description, quantity, unit_price, amount)
    SELECT demo_invoice_id, 'WATER', 'Water usage', 6, 20000, 120000
    WHERE NOT EXISTS (
        SELECT 1 FROM invoice_items WHERE invoice_id = demo_invoice_id AND item_type = 'WATER'
    );

    INSERT INTO invoice_items(invoice_id, item_type, description, quantity, unit_price, amount)
    SELECT demo_invoice_id, 'SERVICE', 'Common service fee', 1, 100000, 100000
    WHERE NOT EXISTS (
        SELECT 1 FROM invoice_items WHERE invoice_id = demo_invoice_id AND item_type = 'SERVICE'
    );

    IF NOT EXISTS (
        SELECT 1
        FROM payments
        WHERE transaction_reference = 'PAY-DEMO-001'
    ) THEN
        INSERT INTO payments(
            invoice_id,
            amount,
            paid_at,
            payment_method,
            payment_status,
            transaction_reference,
            note,
            received_by
        )
        VALUES (
            demo_invoice_id,
            1000000,
            TIMESTAMPTZ '2026-06-25 09:00:00+07',
            'BANK_TRANSFER',
            'COMPLETED',
            'PAY-DEMO-001',
            'Demo partial payment',
            demo_user_id
        );
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM maintenance_requests
        WHERE room_id = room_201_id
          AND title = 'Demo maintenance request'
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
            room_201_id,
            NULL,
            demo_user_id,
            'Demo maintenance request',
            'Air conditioner needs inspection',
            'HIGH',
            'PENDING',
            CURRENT_TIMESTAMP
        );
    END IF;
END $$;
