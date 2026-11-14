-- Jeu d'essai pour les tables existantes (trade, city, user, user_role).
-- Mêmes valeurs que les données fictives de l'interface (ma.fixi.service.mock.MockStore).
-- À exécuter APRÈS schema.sql :  mysql -u root -p < src/main/resources/db/seed.sql
-- Les mots de passe des comptes de démonstration valent « fixi1234 » (hachés avec BCrypt).

USE fixi;

INSERT INTO trade (id, name_fr, name_ar, is_active) VALUES
    (UUID(), 'Plomberie',     'سباكة',          TRUE),
    (UUID(), 'Électricité',   'كهرباء',         TRUE),
    (UUID(), 'Peinture',      'صباغة',          TRUE),
    (UUID(), 'Menuiserie',    'نجارة',          TRUE),
    (UUID(), 'Informatique',  'إعلاميات',       TRUE),
    (UUID(), 'Climatisation', 'تكييف',          TRUE),
    (UUID(), 'Serrurerie',    'أقفال',          TRUE),
    (UUID(), 'Bricolage',     'إصلاحات منزلية', TRUE);

INSERT INTO city (id, name_fr, name_ar, latitude, longitude) VALUES
    (UUID(), 'Agadir',     'أكادير',        30.427800,  -9.598100),
    (UUID(), 'Casablanca', 'الدار البيضاء', 33.573100,  -7.589800),
    (UUID(), 'Fès',        'فاس',           34.018100,  -5.007800),
    (UUID(), 'Marrakech',  'مراكش',         31.629500,  -7.981100),
    (UUID(), 'Rabat',      'الرباط',        34.020900,  -6.841600),
    (UUID(), 'Salé',       'سلا',           34.033100,  -6.798500),
    (UUID(), 'Tanger',     'طنجة',          35.759500,  -5.834000),
    (UUID(), 'Témara',     'تمارة',         33.928700,  -6.906300);

-- Comptes de démonstration. Hachage BCrypt de « fixi1234 » (coût 10).
INSERT INTO user (id, full_name, client_type, email, phone, password_hash, account_status) VALUES
    ('u-client-sarah', 'Sarah Benali',   'PARTICULIER', 'client@fixi.ma',  '+212612345678',
     '$2a$10$6qBIEfXPQ/Drds0ixppNoek13EZQz.qnNvWHxvKlr2BmDVF5ECKrO', 'ACTIVE'),
    ('u-art-ahmed',    'Ahmed El Amine', NULL,          'artisan@fixi.ma', '+212661234567',
     '$2a$10$6qBIEfXPQ/Drds0ixppNoek13EZQz.qnNvWHxvKlr2BmDVF5ECKrO', 'ACTIVE'),
    ('u-admin',        'Équipe Fixi',    NULL,          'admin@fixi.ma',   NULL,
     '$2a$10$6qBIEfXPQ/Drds0ixppNoek13EZQz.qnNvWHxvKlr2BmDVF5ECKrO', 'ACTIVE');

INSERT INTO user_role (user_id, role_code) VALUES
    ('u-client-sarah', 'CLIENT'),
    ('u-art-ahmed',    'ARTISAN'),
    ('u-admin',        'ADMINISTRATOR');
