-- V004: cupones, tramos de tarifa de reparto, ajustes del sitio y carrito.
-- Los cupones se crean ya (se usarán en la versión 1.1) para no migrar después; coste mínimo.

CREATE TABLE coupons (
  id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  public_id         CHAR(26)        NOT NULL,
  code              VARCHAR(40)     NOT NULL,
  description       VARCHAR(160)    NULL,
  coupon_type       VARCHAR(14)     NOT NULL,
  value             DECIMAL(10,2)   NOT NULL DEFAULT 0,
  min_order_amount  DECIMAL(10,2)   NULL,
  max_discount      DECIMAL(10,2)   NULL,
  usage_limit       INT             NULL,
  per_user_limit    INT             NULL,
  used_count        INT             NOT NULL DEFAULT 0,
  scope             VARCHAR(10)     NOT NULL DEFAULT 'ORDER',
  starts_at         DATETIME(3)     NOT NULL,
  ends_at           DATETIME(3)     NULL,
  active            BOOLEAN         NOT NULL DEFAULT TRUE,
  version           INT             NOT NULL DEFAULT 0,
  created_by        BIGINT UNSIGNED NULL,
  updated_by        BIGINT UNSIGNED NULL,
  created_at        DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at        DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uq_coupons_public_id (public_id),
  UNIQUE KEY uq_coupons_code (code),
  KEY idx_coupons_active (active, starts_at, ends_at),
  KEY idx_coupons_created_by (created_by),
  KEY idx_coupons_updated_by (updated_by),
  CONSTRAINT fk_coupons_created_by FOREIGN KEY (created_by) REFERENCES users (id),
  CONSTRAINT fk_coupons_updated_by FOREIGN KEY (updated_by) REFERENCES users (id),
  CONSTRAINT chk_coupons_type CHECK (coupon_type IN ('PERCENT', 'FIXED', 'FREE_SHIPPING')),
  CONSTRAINT chk_coupons_value CHECK (value >= 0),
  CONSTRAINT chk_coupons_percent CHECK (coupon_type <> 'PERCENT' OR value <= 100),
  CONSTRAINT chk_coupons_scope CHECK (scope IN ('ORDER', 'CATEGORY', 'PRODUCT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE coupon_scopes (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  coupon_id    BIGINT UNSIGNED NOT NULL,
  category_id  BIGINT UNSIGNED NULL,
  product_id   BIGINT UNSIGNED NULL,
  PRIMARY KEY (id),
  KEY idx_coupon_scopes_coupon (coupon_id),
  KEY idx_coupon_scopes_category (category_id),
  KEY idx_coupon_scopes_product (product_id),
  CONSTRAINT fk_coupon_scopes_coupon FOREIGN KEY (coupon_id) REFERENCES coupons (id),
  CONSTRAINT fk_coupon_scopes_category FOREIGN KEY (category_id) REFERENCES categories (id),
  CONSTRAINT fk_coupon_scopes_product FOREIGN KEY (product_id) REFERENCES products (id),
  CONSTRAINT chk_coupon_scopes_one_target CHECK ((category_id IS NULL) <> (product_id IS NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Tramos de tarifa por distancia por carretera. La ausencia de solapes y huecos entre tramos la
-- comprueba la aplicación al guardar (MySQL no tiene restricciones de exclusión) y una prueba automática.
CREATE TABLE delivery_fee_bands (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  from_km     DECIMAL(5,1)    NOT NULL,
  to_km       DECIMAL(5,1)    NOT NULL,
  fee         DECIMAL(10,2)   NOT NULL,
  active      BOOLEAN         NOT NULL DEFAULT TRUE,
  created_by  BIGINT UNSIGNED NULL,
  updated_by  BIGINT UNSIGNED NULL,
  created_at  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_delivery_fee_bands_range (active, from_km, to_km),
  KEY idx_delivery_fee_bands_created_by (created_by),
  KEY idx_delivery_fee_bands_updated_by (updated_by),
  CONSTRAINT fk_delivery_fee_bands_created_by FOREIGN KEY (created_by) REFERENCES users (id),
  CONSTRAINT fk_delivery_fee_bands_updated_by FOREIGN KEY (updated_by) REFERENCES users (id),
  CONSTRAINT chk_delivery_fee_bands_fee CHECK (fee >= 0),
  CONSTRAINT chk_delivery_fee_bands_range CHECK (from_km >= 0 AND to_km > from_km)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Ajustes editables por la empresa desde el panel: nada de esto se escribe en el código.
CREATE TABLE app_settings (
  setting_key    VARCHAR(80)  NOT NULL,
  setting_value  TEXT         NULL,
  value_type     VARCHAR(10)  NOT NULL,
  scope          VARCHAR(8)   NOT NULL,
  updated_by     BIGINT UNSIGNED NULL,
  updated_at     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (setting_key),
  KEY idx_app_settings_updated_by (updated_by),
  CONSTRAINT fk_app_settings_updated_by FOREIGN KEY (updated_by) REFERENCES users (id),
  CONSTRAINT chk_app_settings_type CHECK (value_type IN ('STRING', 'NUMBER', 'BOOLEAN', 'COLOR', 'MEDIA', 'JSON')),
  CONSTRAINT chk_app_settings_scope CHECK (scope IN ('SITE', 'SYSTEM'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE carts (
  id                    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  public_id             CHAR(26)        NOT NULL,
  user_id               BIGINT UNSIGNED NULL,
  anonymous_token_hash  CHAR(64)        NULL,
  status                VARCHAR(10)     NOT NULL DEFAULT 'ACTIVE',
  coupon_id             BIGINT UNSIGNED NULL,
  expires_at            DATETIME(3)     NOT NULL,
  -- Un solo carrito activo por usuario.
  active_user_id        BIGINT UNSIGNED GENERATED ALWAYS AS (IF(status = 'ACTIVE', user_id, NULL)) VIRTUAL,
  version               INT             NOT NULL DEFAULT 0,
  created_at            DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at            DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uq_carts_public_id (public_id),
  UNIQUE KEY uq_carts_anonymous_token (anonymous_token_hash),
  UNIQUE KEY uq_carts_one_active (active_user_id),
  KEY idx_carts_user (user_id),
  KEY idx_carts_coupon (coupon_id),
  KEY idx_carts_expiry (status, expires_at),
  CONSTRAINT fk_carts_user FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT fk_carts_coupon FOREIGN KEY (coupon_id) REFERENCES coupons (id),
  CONSTRAINT chk_carts_status CHECK (status IN ('ACTIVE', 'CONVERTED', 'ABANDONED')),
  CONSTRAINT chk_carts_owner CHECK (user_id IS NOT NULL OR anonymous_token_hash IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE cart_items (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  cart_id      BIGINT UNSIGNED NOT NULL,
  product_id   BIGINT UNSIGNED NOT NULL,
  quantity     INT             NOT NULL,
  -- Precio al añadir: solo sirve para avisar de cambios. El precio que cuenta es siempre el vigente.
  added_price  DECIMAL(10,2)   NOT NULL,
  created_at   DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at   DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uq_cart_items (cart_id, product_id),
  KEY idx_cart_items_product (product_id),
  CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES carts (id) ON DELETE CASCADE,
  CONSTRAINT fk_cart_items_product FOREIGN KEY (product_id) REFERENCES products (id),
  CONSTRAINT chk_cart_items_quantity CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
