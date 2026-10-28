-- V002: medios y catálogo. Imágenes con origen y derechos declarados, categorías, marcas (solo texto),
-- tipos de IVA, productos, códigos de barras, alérgenos e historial de precios.

CREATE TABLE media_assets (
  id                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  public_id            CHAR(26)        NOT NULL,
  media_type           VARCHAR(12)     NOT NULL DEFAULT 'IMAGE',
  storage_key          VARCHAR(255)    NOT NULL,
  mime_type            VARCHAR(60)     NOT NULL,
  width                INT UNSIGNED    NULL,
  height               INT UNSIGNED    NULL,
  size_bytes           INT UNSIGNED    NOT NULL,
  variants             JSON            NULL,
  alt_text             VARCHAR(200)    NULL,
  -- Política de imágenes (Fase 4, apartado 15): todo medio declara de dónde sale y con qué derechos.
  origin               VARCHAR(20)     NOT NULL,
  license              VARCHAR(60)     NULL,
  author               VARCHAR(120)    NULL,
  attribution_text     VARCHAR(255)    NULL,
  source_url           VARCHAR(500)    NULL,
  rights_confirmed_by  BIGINT UNSIGNED NULL,
  rights_confirmed_at  DATETIME(3)     NULL,
  uploaded_by          BIGINT UNSIGNED NULL,
  deleted_at           DATETIME(3)     NULL,
  created_at           DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at           DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uq_media_assets_public_id (public_id),
  UNIQUE KEY uq_media_assets_storage_key (storage_key),
  KEY idx_media_assets_origin (origin),
  KEY idx_media_assets_pending_rights (rights_confirmed_at),
  KEY idx_media_assets_rights_by (rights_confirmed_by),
  KEY idx_media_assets_uploaded_by (uploaded_by),
  CONSTRAINT fk_media_assets_rights_by FOREIGN KEY (rights_confirmed_by) REFERENCES users (id),
  CONSTRAINT fk_media_assets_uploaded_by FOREIGN KEY (uploaded_by) REFERENCES users (id),
  CONSTRAINT chk_media_assets_origin CHECK (origin IN
    ('OWN', 'STOCK_FREE', 'COMMONS', 'OPEN_FOOD_FACTS', 'SUPPLIER', 'ILLUSTRATION', 'AI_GENERIC'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE categories (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  public_id       CHAR(26)        NOT NULL,
  parent_id       BIGINT UNSIGNED NULL,
  name            VARCHAR(80)     NOT NULL,
  slug            VARCHAR(100)    NOT NULL,
  -- Ruta materializada, por ejemplo '/1/5/': permite pedir todo un subárbol con una sola consulta.
  path            VARCHAR(255)    NOT NULL,
  depth           TINYINT UNSIGNED NOT NULL,
  position        INT             NOT NULL DEFAULT 0,
  active          BOOLEAN         NOT NULL DEFAULT TRUE,
  image_media_id  BIGINT UNSIGNED NULL,
  created_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uq_categories_public_id (public_id),
  UNIQUE KEY uq_categories_slug (slug),
  KEY idx_categories_parent (parent_id, position),
  KEY idx_categories_path (path),
  KEY idx_categories_image (image_media_id),
  CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES categories (id),
  CONSTRAINT fk_categories_image FOREIGN KEY (image_media_id) REFERENCES media_assets (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Marcas: solo nombre. No se guardan logotipos (política de marcas, Fase 4 apartado 15.3).
CREATE TABLE brands (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  name        VARCHAR(80)     NOT NULL,
  slug        VARCHAR(100)    NOT NULL,
  active      BOOLEAN         NOT NULL DEFAULT TRUE,
  created_at  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uq_brands_name (name),
  UNIQUE KEY uq_brands_slug (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE tax_rates (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  code        VARCHAR(20)     NOT NULL,
  name        VARCHAR(60)     NOT NULL,
  rate        DECIMAL(5,2)    NOT NULL,
  valid_from  DATE            NOT NULL,
  valid_to    DATE            NULL,
  created_at  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uq_tax_rates_code (code),
  CONSTRAINT chk_tax_rates_rate CHECK (rate >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE products (
  id                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  public_id          CHAR(26)        NOT NULL,
  sku                VARCHAR(40)     NOT NULL,
  slug               VARCHAR(160)    NOT NULL,
  category_id        BIGINT UNSIGNED NOT NULL,
  brand_id           BIGINT UNSIGNED NULL,
  tax_rate_id        BIGINT UNSIGNED NOT NULL,
  name               VARCHAR(160)    NOT NULL,
  short_description  VARCHAR(255)    NULL,
  description        TEXT            NULL,
  format_label       VARCHAR(60)     NOT NULL,
  price              DECIMAL(10,2)   NOT NULL,
  previous_price     DECIMAL(10,2)   NULL,
  unit_type          VARCHAR(10)     NOT NULL DEFAULT 'UNIT',
  content_quantity   DECIMAL(10,3)   NOT NULL,
  content_unit       VARCHAR(4)      NOT NULL,
  -- Precio por kg, litro o unidad: calculado, nunca se desincroniza del precio.
  price_per_unit     DECIMAL(10,2) GENERATED ALWAYS AS (ROUND(price / content_quantity, 2)) STORED,
  age_restricted     BOOLEAN         NOT NULL DEFAULT FALSE,
  local_product      BOOLEAN         NOT NULL DEFAULT FALSE,
  max_per_order      SMALLINT UNSIGNED NOT NULL DEFAULT 12,
  status             VARCHAR(10)     NOT NULL DEFAULT 'DRAFT',
  published_at       DATETIME(3)     NULL,
  version            INT             NOT NULL DEFAULT 0,
  created_by         BIGINT UNSIGNED NULL,
  updated_by         BIGINT UNSIGNED NULL,
  created_at         DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at         DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uq_products_public_id (public_id),
  UNIQUE KEY uq_products_sku (sku),
  UNIQUE KEY uq_products_slug (slug),
  KEY idx_products_list (category_id, status, name),
  KEY idx_products_brand (brand_id),
  KEY idx_products_tax_rate (tax_rate_id),
  KEY idx_products_offers (status, previous_price),
  KEY idx_products_local (status, local_product),
  KEY idx_products_created_by (created_by),
  KEY idx_products_updated_by (updated_by),
  CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id),
  CONSTRAINT fk_products_brand FOREIGN KEY (brand_id) REFERENCES brands (id),
  CONSTRAINT fk_products_tax_rate FOREIGN KEY (tax_rate_id) REFERENCES tax_rates (id),
  CONSTRAINT fk_products_created_by FOREIGN KEY (created_by) REFERENCES users (id),
  CONSTRAINT fk_products_updated_by FOREIGN KEY (updated_by) REFERENCES users (id),
  CONSTRAINT chk_products_price CHECK (price > 0),
  CONSTRAINT chk_products_offer CHECK (previous_price IS NULL OR previous_price > price),
  CONSTRAINT chk_products_content CHECK (content_quantity > 0),
  CONSTRAINT chk_products_unit_type CHECK (unit_type IN ('UNIT', 'WEIGHT', 'VOLUME')),
  CONSTRAINT chk_products_content_unit CHECK (content_unit IN ('kg', 'L', 'ud')),
  CONSTRAINT chk_products_status CHECK (status IN ('DRAFT', 'ACTIVE', 'HIDDEN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE product_barcodes (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  product_id    BIGINT UNSIGNED NOT NULL,
  barcode       VARCHAR(14)     NOT NULL,
  barcode_type  VARCHAR(10)     NOT NULL DEFAULT 'EAN13',
  created_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uq_product_barcodes_barcode (barcode),
  KEY idx_product_barcodes_product (product_id),
  CONSTRAINT fk_product_barcodes_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Imágenes de producto. Sin ON DELETE CASCADE: product_id es columna base de una columna calculada
-- y MySQL no permite acciones referenciales en cascada sobre ella. Los productos nunca se borran.
CREATE TABLE product_images (
  id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  product_id          BIGINT UNSIGNED NOT NULL,
  media_id            BIGINT UNSIGNED NOT NULL,
  position            SMALLINT        NOT NULL DEFAULT 0,
  is_primary          BOOLEAN         NOT NULL DEFAULT FALSE,
  -- Una sola imagen principal por producto.
  primary_product_id  BIGINT UNSIGNED GENERATED ALWAYS AS (IF(is_primary, product_id, NULL)) VIRTUAL,
  created_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uq_product_images (product_id, media_id),
  UNIQUE KEY uq_product_images_primary (primary_product_id),
  KEY idx_product_images_media (media_id),
  CONSTRAINT fk_product_images_product FOREIGN KEY (product_id) REFERENCES products (id),
  CONSTRAINT fk_product_images_media FOREIGN KEY (media_id) REFERENCES media_assets (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE allergens (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  code        VARCHAR(30)     NOT NULL,
  name        VARCHAR(60)     NOT NULL,
  created_at  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uq_allergens_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE product_allergens (
  product_id   BIGINT UNSIGNED NOT NULL,
  allergen_id  BIGINT UNSIGNED NOT NULL,
  relation     VARCHAR(12)     NOT NULL,
  PRIMARY KEY (product_id, allergen_id),
  KEY idx_product_allergens_allergen (allergen_id),
  CONSTRAINT fk_product_allergens_product FOREIGN KEY (product_id) REFERENCES products (id),
  CONSTRAINT fk_product_allergens_allergen FOREIGN KEY (allergen_id) REFERENCES allergens (id),
  CONSTRAINT chk_product_allergens_relation CHECK (relation IN ('CONTAINS', 'MAY_CONTAIN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Historial de precios: solo inserción (ver V010).
CREATE TABLE product_price_history (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  product_id  BIGINT UNSIGNED NOT NULL,
  old_price   DECIMAL(10,2)   NULL,
  new_price   DECIMAL(10,2)   NOT NULL,
  changed_by  BIGINT UNSIGNED NULL,
  created_at  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_price_history_product (product_id, created_at),
  KEY idx_price_history_changed_by (changed_by),
  CONSTRAINT fk_price_history_product FOREIGN KEY (product_id) REFERENCES products (id),
  CONSTRAINT fk_price_history_changed_by FOREIGN KEY (changed_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
