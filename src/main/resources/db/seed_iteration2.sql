SET NAMES utf8mb4;
USE fixi;

-- Insertion des comptes utilisateurs artisans supplémentaires (mot de passe 'fixi1234')
INSERT IGNORE INTO user (id, full_name, client_type, email, phone, password_hash, account_status) VALUES
('u-art-fatima',  'Fatima Zahra Alaoui', NULL, 'fatima@fixi.ma',  '+212662000001', '$2a$10$6qBIEfXPQ/Drds0ixppNoek13EZQz.qnNvWHxvKlr2BmDVF5ECKrO', 'ACTIVE'),
('u-art-youssef', 'Youssef Bensli',      NULL, 'youssef@fixi.ma', '+212662000002', '$2a$10$6qBIEfXPQ/Drds0ixppNoek13EZQz.qnNvWHxvKlr2BmDVF5ECKrO', 'ACTIVE'),
('u-art-omar',    'Omar Idrissi',        NULL, 'omar@fixi.ma',    '+212662000003', '$2a$10$6qBIEfXPQ/Drds0ixppNoek13EZQz.qnNvWHxvKlr2BmDVF5ECKrO', 'ACTIVE'),
('u-art-hicham',  'Hicham Tazi',         NULL, 'hicham@fixi.ma',  '+212662000004', '$2a$10$6qBIEfXPQ/Drds0ixppNoek13EZQz.qnNvWHxvKlr2BmDVF5ECKrO', 'ACTIVE'),
('u-art-nadia',   'Nadia Chraibi',       NULL, 'nadia@fixi.ma',   '+212662000005', '$2a$10$6qBIEfXPQ/Drds0ixppNoek13EZQz.qnNvWHxvKlr2BmDVF5ECKrO', 'ACTIVE'),
('u-art-rachid',  'Rachid Ouazzani',     NULL, 'rachid@fixi.ma',  '+212662000006', '$2a$10$6qBIEfXPQ/Drds0ixppNoek13EZQz.qnNvWHxvKlr2BmDVF5ECKrO', 'ACTIVE'),
('u-art-said',    'Saïd Amrani',         NULL, 'said@fixi.ma',    '+212662000007', '$2a$10$6qBIEfXPQ/Drds0ixppNoek13EZQz.qnNvWHxvKlr2BmDVF5ECKrO', 'ACTIVE'),
('u-art-imane',   'Imane Berrada',       NULL, 'imane@fixi.ma',   '+212662000008', '$2a$10$6qBIEfXPQ/Drds0ixppNoek13EZQz.qnNvWHxvKlr2BmDVF5ECKrO', 'ACTIVE'),
('u-art-khalid',  'Khalid Naciri',       NULL, 'khalid@fixi.ma',  '+212662000009', '$2a$10$6qBIEfXPQ/Drds0ixppNoek13EZQz.qnNvWHxvKlr2BmDVF5ECKrO', 'ACTIVE'),
('u-art-mehdi',   'Mehdi Fassi',         NULL, 'mehdi@fixi.ma',   '+212662000010', '$2a$10$6qBIEfXPQ/Drds0ixppNoek13EZQz.qnNvWHxvKlr2BmDVF5ECKrO', 'ACTIVE'),
('u-art-amina',   'Amina Kettani',       NULL, 'amina@fixi.ma',   '+212662000011', '$2a$10$6qBIEfXPQ/Drds0ixppNoek13EZQz.qnNvWHxvKlr2BmDVF5ECKrO', 'ACTIVE');

INSERT IGNORE INTO user_role (user_id, role_code) VALUES
('u-art-fatima', 'ARTISAN'),
('u-art-youssef', 'ARTISAN'),
('u-art-omar', 'ARTISAN'),
('u-art-hicham', 'ARTISAN'),
('u-art-nadia', 'ARTISAN'),
('u-art-rachid', 'ARTISAN'),
('u-art-said', 'ARTISAN'),
('u-art-imane', 'ARTISAN'),
('u-art-khalid', 'ARTISAN'),
('u-art-mehdi', 'ARTISAN'),
('u-art-amina', 'ARTISAN');

-- Profils artisans
INSERT INTO artisan_profile (user_id, display_name, description, city_id, years_of_experience, verification_status, average_rating, review_count, next_available_on)
SELECT 'u-art-ahmed', 'Ahmed El Amine', 'Plombier depuis 5 ans. Fuites, chauffe-eau, sanitaires et installations neuves. Devis gratuit et détaillé avant chaque intervention.',
       c.id, 5, 'VERIFIED', 4.80, 120, CURDATE()
FROM city c WHERE c.name_fr LIKE 'Rabat%' LIMIT 1
ON DUPLICATE KEY UPDATE display_name = VALUES(display_name);

INSERT INTO artisan_profile (user_id, display_name, description, city_id, years_of_experience, verification_status, average_rating, review_count, next_available_on)
SELECT 'u-art-fatima', 'Fatima Zahra Alaoui', 'Électricienne certifiée : mise aux normes, tableaux électriques, éclairage et dépannage.',
       c.id, 4, 'VERIFIED', 4.70, 98, DATE_ADD(CURDATE(), INTERVAL 1 DAY)
FROM city c WHERE c.name_fr LIKE 'Casablanca%' LIMIT 1
ON DUPLICATE KEY UPDATE display_name = VALUES(display_name);

INSERT INTO artisan_profile (user_id, display_name, description, city_id, years_of_experience, verification_status, average_rating, review_count, next_available_on)
SELECT 'u-art-youssef', 'Youssef Bensli', 'Peinture intérieure et extérieure, enduits décoratifs et ravalement de façades.',
       c.id, 7, 'VERIFIED', 4.90, 170, CURDATE()
FROM city c WHERE c.name_fr LIKE 'Sal%' LIMIT 1
ON DUPLICATE KEY UPDATE display_name = VALUES(display_name);

INSERT INTO artisan_profile (user_id, display_name, description, city_id, years_of_experience, verification_status, average_rating, review_count, next_available_on)
SELECT 'u-art-omar', 'Omar Idrissi', 'Réparation d\'ordinateurs, installation de réseaux et récupération de données.',
       c.id, 6, 'VERIFIED', 4.60, 54, DATE_ADD(CURDATE(), INTERVAL 3 DAY)
FROM city c WHERE c.name_fr LIKE 'Rabat%' LIMIT 1
ON DUPLICATE KEY UPDATE display_name = VALUES(display_name);

INSERT INTO artisan_profile (user_id, display_name, description, city_id, years_of_experience, verification_status, average_rating, review_count, next_available_on)
SELECT 'u-art-hicham', 'Hicham Tazi', 'Dépannage plomberie 7j/7, débouchage et recherche de fuites.',
       c.id, 9, 'VERIFIED', 4.50, 63, DATE_ADD(CURDATE(), INTERVAL 1 DAY)
FROM city c WHERE c.name_fr LIKE 'Casablanca%' LIMIT 1
ON DUPLICATE KEY UPDATE display_name = VALUES(display_name);

INSERT INTO artisan_profile (user_id, display_name, description, city_id, years_of_experience, verification_status, average_rating, review_count, next_available_on)
SELECT 'u-art-nadia', 'Nadia Chraibi', 'Installation et entretien de climatiseurs et pompes à chaleur, toutes marques.',
       c.id, 8, 'VERIFIED', 4.80, 41, DATE_ADD(CURDATE(), INTERVAL 4 DAY)
FROM city c WHERE c.name_fr LIKE 'Marrakech%' LIMIT 1
ON DUPLICATE KEY UPDATE display_name = VALUES(display_name);

-- Métiers des artisans (artisan_trade)
INSERT IGNORE INTO artisan_trade (artisan_id, trade_id, indicative_rate_min, indicative_rate_max)
SELECT 'u-art-ahmed', t.id, 150.00, 300.00 FROM trade t WHERE t.name_fr LIKE 'Plomb%' LIMIT 1;

INSERT IGNORE INTO artisan_trade (artisan_id, trade_id, indicative_rate_min, indicative_rate_max)
SELECT 'u-art-fatima', t.id, 200.00, 400.00 FROM trade t WHERE t.name_fr LIKE '%lectr%' LIMIT 1;

INSERT IGNORE INTO artisan_trade (artisan_id, trade_id, indicative_rate_min, indicative_rate_max)
SELECT 'u-art-youssef', t.id, 120.00, 250.00 FROM trade t WHERE t.name_fr LIKE 'Peint%' LIMIT 1;

INSERT IGNORE INTO artisan_trade (artisan_id, trade_id, indicative_rate_min, indicative_rate_max)
SELECT 'u-art-omar', t.id, 180.00, 350.00 FROM trade t WHERE t.name_fr LIKE 'Inform%' LIMIT 1;

INSERT IGNORE INTO artisan_trade (artisan_id, trade_id, indicative_rate_min, indicative_rate_max)
SELECT 'u-art-hicham', t.id, 130.00, 280.00 FROM trade t WHERE t.name_fr LIKE 'Plomb%' LIMIT 1;

INSERT IGNORE INTO artisan_trade (artisan_id, trade_id, indicative_rate_min, indicative_rate_max)
SELECT 'u-art-nadia', t.id, 300.00, 600.00 FROM trade t WHERE t.name_fr LIKE 'Clim%' LIMIT 1;

-- Zones couvertes (artisan_coverage)
INSERT IGNORE INTO artisan_coverage (artisan_id, city_id)
SELECT 'u-art-ahmed', c.id FROM city c WHERE c.name_fr LIKE 'Rabat%' OR c.name_fr LIKE 'Sal%' OR c.name_fr LIKE 'T%mara%';

INSERT IGNORE INTO artisan_coverage (artisan_id, city_id)
SELECT 'u-art-youssef', c.id FROM city c WHERE c.name_fr LIKE 'Sal%' OR c.name_fr LIKE 'Rabat%';

-- Demandes d'exemple
INSERT INTO service_request (id, reference, client_id, trade_id, city_id, title, description, budget, preferred_date, status, assigned_artisan_id, accepted_quote_total, created_at)
SELECT 'req-fuite-evier', 'D-4587', 'u-client-sarah', t.id, c.id,
       'Fuite d\'eau sous l\'évier', 'Une fuite au niveau du siphon de l\'évier de la cuisine, l\'eau coule en continu.',
       400.00, DATE_SUB(CURDATE(), INTERVAL 1 DAY), 'DONE_BY_ARTISAN', 'u-art-ahmed', 350.00, DATE_SUB(NOW(), INTERVAL 6 DAY)
FROM trade t, city c WHERE t.name_fr LIKE 'Plomb%' AND c.name_fr LIKE 'Sal%' LIMIT 1
ON DUPLICATE KEY UPDATE title = VALUES(title);

INSERT INTO service_request (id, reference, client_id, trade_id, city_id, title, description, budget, preferred_date, status, assigned_artisan_id, accepted_quote_total, created_at)
SELECT 'req-peinture-salon', 'D-4588', 'u-client-sarah', t.id, c.id,
       'Peinture salon', 'Repeindre un salon de 25 m², murs et plafond, couleur blanc cassé.',
       2500.00, DATE_ADD(CURDATE(), INTERVAL 5 DAY), 'SCHEDULED', 'u-art-youssef', 2400.00, DATE_SUB(NOW(), INTERVAL 9 DAY)
FROM trade t, city c WHERE t.name_fr LIKE 'Peint%' AND c.name_fr LIKE 'Rabat%' LIMIT 1
ON DUPLICATE KEY UPDATE title = VALUES(title);

INSERT INTO service_request (id, reference, client_id, trade_id, city_id, title, description, budget, preferred_date, status, assigned_artisan_id, accepted_quote_total, created_at)
SELECT 'req-pc-panne', 'D-4589', 'u-client-sarah', t.id, c.id,
       'Réparation ordinateur', 'L\'ordinateur portable ne s\'allume plus depuis hier, le voyant de charge reste éteint.',
       500.00, DATE_ADD(CURDATE(), INTERVAL 1 DAY), 'IN_PROGRESS', 'u-art-omar', 450.00, DATE_SUB(NOW(), INTERVAL 3 DAY)
FROM trade t, city c WHERE t.name_fr LIKE 'Inform%' AND c.name_fr LIKE 'Rabat%' LIMIT 1
ON DUPLICATE KEY UPDATE title = VALUES(title);

-- Destinataires et demandes entrantes
INSERT IGNORE INTO request_recipient (request_id, artisan_id, sent_at, response_status) VALUES
('req-fuite-evier', 'u-art-ahmed', NOW(), 'QUOTED');

-- Rendez-vous à venir pour Ahmed
INSERT IGNORE INTO appointment (id, artisan_id, request_id, starts_at, ends_at) VALUES
(UUID(), 'u-art-ahmed', 'req-fuite-evier', DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 2 DAY));

-- Avis clients
INSERT IGNORE INTO review (id, artisan_id, request_id, author_id, rating, comment, published_on) VALUES
(UUID(), 'u-art-ahmed', 'req-fuite-evier', 'u-client-sarah', 5, 'Ponctuel, travail très propre et tarif conforme au devis.', DATE_SUB(CURDATE(), INTERVAL 2 DAY));

-- Conversation d'exemple
INSERT INTO conversation (id, request_id, client_id, artisan_id, created_at) VALUES
('conv-sarah-ahmed', 'req-fuite-evier', 'u-client-sarah', 'u-art-ahmed', DATE_SUB(NOW(), INTERVAL 5 DAY))
ON DUPLICATE KEY UPDATE created_at = VALUES(created_at);

INSERT IGNORE INTO message (id, conversation_id, sender_id, body, sent_at) VALUES
(UUID(), 'conv-sarah-ahmed', 'u-client-sarah', 'Bonjour M. El Amine, pouvez-vous intervenir rapidement pour la fuite ?', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(UUID(), 'conv-sarah-ahmed', 'u-art-ahmed', 'Bonjour Mme Benali, oui je peux passer demain matin vers 10h pour vérifier les raccords.', DATE_SUB(NOW(), INTERVAL 4 DAY));
