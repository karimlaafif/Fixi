-- Schéma compatible PostgreSQL pour Fixi (Itération 2)
-- Exécuter avec : psql -U postgres -d fixi -f src/main/resources/db/schema_postgres.sql

CREATE TABLE IF NOT EXISTS "user" (
    id              CHAR(36)     PRIMARY KEY,
    full_name       VARCHAR(150) NOT NULL,
    client_type     VARCHAR(30)  NULL,
    email           VARCHAR(190) NULL UNIQUE,
    phone           VARCHAR(20)  NULL UNIQUE,
    password_hash   VARCHAR(100) NOT NULL,
    account_status  VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_contact CHECK (email IS NOT NULL OR phone IS NOT NULL)
);

CREATE TABLE IF NOT EXISTS user_role (
    user_id     CHAR(36)    NOT NULL,
    role_code   VARCHAR(30) NOT NULL,
    assigned_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_code),
    FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS trade (
    id        CHAR(36)     PRIMARY KEY,
    name_fr   VARCHAR(100) NOT NULL,
    name_ar   VARCHAR(100) NOT NULL,
    is_active BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS city (
    id        CHAR(36)      PRIMARY KEY,
    name_fr   VARCHAR(100)  NOT NULL,
    name_ar   VARCHAR(100)  NOT NULL,
    latitude  NUMERIC(9, 6) NOT NULL,
    longitude NUMERIC(9, 6) NOT NULL,
    is_active BOOLEAN       NOT NULL DEFAULT TRUE
);
