CREATE DATABASE IF NOT EXISTS fixi CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE fixi;

CREATE TABLE IF NOT EXISTS user (
    id              CHAR(36)     PRIMARY KEY,
    full_name       VARCHAR(150) NOT NULL,
    client_type     ENUM('PARTICULIER', 'SYNDIC') NULL,
    email           VARCHAR(190) NULL UNIQUE,
    phone           VARCHAR(20)  NULL UNIQUE,
    password_hash   VARCHAR(100) NOT NULL,
    account_status  ENUM('PENDING', 'ACTIVE', 'SUSPENDED') NOT NULL DEFAULT 'PENDING',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_contact CHECK (email IS NOT NULL OR phone IS NOT NULL)
);

CREATE TABLE IF NOT EXISTS user_role (
    user_id     CHAR(36) NOT NULL,
    role_code   ENUM('CLIENT', 'ARTISAN', 'ADMINISTRATOR', 'SUPPORT') NOT NULL,
    assigned_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_code),
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
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
    latitude  DECIMAL(9, 6) NOT NULL,
    longitude DECIMAL(9, 6) NOT NULL,
    is_active BOOLEAN       NOT NULL DEFAULT TRUE
);
