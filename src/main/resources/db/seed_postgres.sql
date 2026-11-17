-- Jeu d'essai pour PostgreSQL (trade, city, user, user_role)
-- Exécuter avec : psql -U postgres -d fixi -f src/main/resources/db/seed_postgres.sql

INSERT INTO trade (id, name_fr, name_ar, is_active) VALUES
    (gen_random_uuid()::text, 'Plomberie',     'سباكة',          TRUE),
    (gen_random_uuid()::text, 'Électricité',   'كهرباء',         TRUE),
    (gen_random_uuid()::text, 'Peinture',      'صباغة',          TRUE),
    (gen_random_uuid()::text, 'Menuiserie',    'نجارة',          TRUE),
    (gen_random_uuid()::text, 'Informatique',  'إعلاميات',       TRUE),
    (gen_random_uuid()::text, 'Climatisation', 'تكييف',          TRUE),
    (gen_random_uuid()::text, 'Serrurerie',    'أقفال',          TRUE),
    (gen_random_uuid()::text, 'Bricolage',     'إصلاحات منزلية', TRUE)
ON CONFLICT DO NOTHING;

INSERT INTO city (id, name_fr, name_ar, latitude, longitude) VALUES
    (gen_random_uuid()::text, 'Agadir',     'أكادير',        30.427800,  -9.598100),
    (gen_random_uuid()::text, 'Casablanca', 'الدار البيضاء', 33.573100,  -7.589800),
    (gen_random_uuid()::text, 'Fès',        'فاس',           34.018100,  -5.007800),
    (gen_random_uuid()::text, 'Marrakech',  'مراكش',         31.629500,  -7.981100),
    (gen_random_uuid()::text, 'Rabat',      'الرباط',        34.020900,  -6.841600),
    (gen_random_uuid()::text, 'Salé',       'سلا',           34.033100,  -6.798500),
    (gen_random_uuid()::text, 'Tanger',     'طنجة',          35.759500,  -5.834000),
    (gen_random_uuid()::text, 'Témara',     'تمارة',         33.928700,  -6.906300)
ON CONFLICT DO NOTHING;

-- Comptes de démonstration avec hash BCrypt de « fixi1234 »
INSERT INTO "user" (id, full_name, client_type, email, phone, password_hash, account_status) VALUES
    ('u-client-sarah', 'Sarah Benali',   'PARTICULIER', 'client@fixi.ma',  '+212612345678',
     '$2a$10$6qBIEfXPQ/Drds0ixppNoek13EZQz.qnNvWHxvKlr2BmDVF5ECKrO', 'ACTIVE'),
    ('u-art-ahmed',    'Ahmed El Amine', NULL,          'artisan@fixi.ma', '+212661234567',
     '$2a$10$6qBIEfXPQ/Drds0ixppNoek13EZQz.qnNvWHxvKlr2BmDVF5ECKrO', 'ACTIVE'),
    ('u-admin',        'Équipe Fixi',    NULL,          'admin@fixi.ma',   NULL,
     '$2a$10$6qBIEfXPQ/Drds0ixppNoek13EZQz.qnNvWHxvKlr2BmDVF5ECKrO', 'ACTIVE')
ON CONFLICT DO NOTHING;

INSERT INTO user_role (user_id, role_code) VALUES
    ('u-client-sarah', 'CLIENT'),
    ('u-art-ahmed',    'ARTISAN'),
    ('u-admin',        'ADMINISTRATOR')
ON CONFLICT DO NOTHING;
