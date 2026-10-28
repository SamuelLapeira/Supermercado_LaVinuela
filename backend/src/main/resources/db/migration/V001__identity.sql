-- V001: identidad. Usuarios, roles y permisos, sesiones, tokens de un solo uso y direcciones.
-- Convenciones (docs/database.md): InnoDB, utf8mb4, fechas DATETIME(3) en UTC, dinero DECIMAL(10,2),
-- estados como VARCHAR + CHECK, todas las claves foráneas con su índice.

CREATE TABLE users (
  id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  public_id           CHAR(26)        NOT NULL,
  email               VARCHAR(254)    NOT NULL,
  password_hash       VARCHAR(255)    NOT NULL,
  name                VARCHAR(80)     NOT NULL,
  surname             VARCHAR(120)    NOT NULL,
  phone               VARCHAR(20)     NULL,
  status              VARCHAR(24)     NOT NULL DEFAULT 'PENDING_VERIFICATION',
  email_verified_at   DATETIME(3)     NULL,
  failed_login_count  SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  locked_until        DATETIME(3)     NULL,
  last_login_at       DATETIME(3)     NULL,
  marketing_consent   BOOLEAN         NOT NULL DEFAULT FALSE,
  anonymized_at       DATETIME(3)     NULL,
  version             INT             NOT NULL DEFAULT 0,
  created_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uq_users_public_id (public_id),
  UNIQUE KEY uq_users_email (email),
  KEY idx_users_status (status),
  CONSTRAINT chk_users_status CHECK (status IN ('PENDING_VERIFICATION', 'ACTIVE', 'LOCKED', 'DELETED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Prueba de consentimientos (RGPD): qué versión de qué documento aceptó cada persona y cuándo.
CREATE TABLE user_consents (
  id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id           BIGINT UNSIGNED NOT NULL,
  consent_type      VARCHAR(16)     NOT NULL,
  document_version  VARCHAR(20)     NOT NULL,
  granted           BOOLEAN         NOT NULL,
  ip                VARBINARY(16)   NULL,
  created_at        DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at        DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_user_consents_user (user_id, consent_type, created_at),
  CONSTRAINT fk_user_consents_user FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT chk_user_consents_type CHECK (consent_type IN ('TERMS', 'PRIVACY', 'MARKETING'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE roles (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  code        VARCHAR(30)     NOT NULL,
  name        VARCHAR(60)     NOT NULL,
  created_at  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uq_roles_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE permissions (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  code         VARCHAR(40)     NOT NULL,
  description  VARCHAR(200)    NOT NULL,
  created_at   DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at   DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uq_permissions_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE role_permissions (
  role_id        BIGINT UNSIGNED NOT NULL,
  permission_id  BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (role_id, permission_id),
  KEY idx_role_permissions_permission (permission_id),
  CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles (id),
  CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE user_roles (
  user_id  BIGINT UNSIGNED NOT NULL,
  role_id  BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (user_id, role_id),
  KEY idx_user_roles_role (role_id),
  CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Segundo factor del personal (TOTP). El secreto se guarda cifrado por la aplicación.
CREATE TABLE mfa_credentials (
  id                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id              BIGINT UNSIGNED NOT NULL,
  secret_encrypted     VARBINARY(255)  NOT NULL,
  confirmed_at         DATETIME(3)     NULL,
  recovery_codes_hash  JSON            NULL,
  created_at           DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at           DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uq_mfa_credentials_user (user_id),
  CONSTRAINT fk_mfa_credentials_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Tokens de refresco: opacos, de un solo uso y con rotación. Solo se guarda su hash (SHA-256).
CREATE TABLE refresh_tokens (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id         BIGINT UNSIGNED NOT NULL,
  token_hash      CHAR(64)        NOT NULL,
  family_id       CHAR(26)        NOT NULL,
  expires_at      DATETIME(3)     NOT NULL,
  revoked_at      DATETIME(3)     NULL,
  replaced_by_id  BIGINT UNSIGNED NULL,
  ip              VARBINARY(16)   NULL,
  user_agent      VARCHAR(255)    NULL,
  created_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uq_refresh_tokens_hash (token_hash),
  KEY idx_refresh_tokens_user (user_id),
  KEY idx_refresh_tokens_family (family_id),
  KEY idx_refresh_tokens_expires (expires_at),
  KEY idx_refresh_tokens_replaced (replaced_by_id),
  CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT fk_refresh_tokens_replaced FOREIGN KEY (replaced_by_id) REFERENCES refresh_tokens (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Verificación de correo y recuperación de contraseña: un solo uso y con caducidad.
CREATE TABLE user_tokens (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id     BIGINT UNSIGNED NOT NULL,
  token_type  VARCHAR(20)     NOT NULL,
  token_hash  CHAR(64)        NOT NULL,
  expires_at  DATETIME(3)     NOT NULL,
  used_at     DATETIME(3)     NULL,
  created_at  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uq_user_tokens_hash (token_hash),
  KEY idx_user_tokens_user (user_id, token_type),
  KEY idx_user_tokens_expires (expires_at),
  CONSTRAINT fk_user_tokens_user FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT chk_user_tokens_type CHECK (token_type IN ('EMAIL_VERIFICATION', 'PASSWORD_RESET'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE addresses (
  id                      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  public_id               CHAR(26)        NOT NULL,
  user_id                 BIGINT UNSIGNED NOT NULL,
  label                   VARCHAR(40)     NULL,
  recipient_name          VARCHAR(120)    NOT NULL,
  phone                   VARCHAR(20)     NULL,
  street                  VARCHAR(150)    NOT NULL,
  street_number           VARCHAR(12)     NULL,
  floor_door              VARCHAR(30)     NULL,
  postal_code             CHAR(5)         NOT NULL,
  city                    VARCHAR(80)     NOT NULL,
  province                VARCHAR(60)     NOT NULL,
  country_code            CHAR(2)         NOT NULL DEFAULT 'ES',
  delivery_notes          VARCHAR(255)    NULL,
  latitude                DECIMAL(9,6)    NULL,
  longitude               DECIMAL(9,6)    NULL,
  road_distance_km        DECIMAL(5,1)    NULL,
  distance_provider       VARCHAR(30)     NULL,
  distance_calculated_at  DATETIME(3)     NULL,
  is_default              BOOLEAN         NOT NULL DEFAULT FALSE,
  deleted_at              DATETIME(3)     NULL,
  -- Como mucho una dirección por defecto por usuario: columna calculada + índice único (varios NULL no chocan).
  default_user_id         BIGINT UNSIGNED GENERATED ALWAYS AS (IF(is_default AND deleted_at IS NULL, user_id, NULL)) VIRTUAL,
  version                 INT             NOT NULL DEFAULT 0,
  created_at              DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at              DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uq_addresses_public_id (public_id),
  UNIQUE KEY uq_addresses_one_default (default_user_id),
  KEY idx_addresses_user (user_id, deleted_at),
  CONSTRAINT fk_addresses_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
