-- V003: inventario. Existencias, reservas con caducidad y movimientos (solo inserción, ver V010).
-- Cantidades enteras: en el MVP todo se vende en formatos de precio fijo (si algún día se vende por
-- peso variable habrá que migrar a decimales).

CREATE TABLE stock_items (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  product_id     BIGINT UNSIGNED NOT NULL,
  on_hand        INT             NOT NULL DEFAULT 0,
  reserved       INT             NOT NULL DEFAULT 0,
  safety_stock   INT             NOT NULL DEFAULT 0,
  reorder_level  INT             NOT NULL DEFAULT 0,
  version        INT             NOT NULL DEFAULT 0,
  created_at     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uq_stock_items_product (product_id),
  KEY idx_stock_items_low (reorder_level, on_hand),
  CONSTRAINT fk_stock_items_product FOREIGN KEY (product_id) REFERENCES products (id),
  CONSTRAINT chk_stock_on_hand CHECK (on_hand >= 0),
  CONSTRAINT chk_stock_reserved CHECK (reserved >= 0),
  CONSTRAINT chk_stock_safety CHECK (safety_stock >= 0 AND reorder_level >= 0),
  CONSTRAINT chk_stock_reserved_le_on_hand CHECK (reserved <= on_hand)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- La clave foránea de order_id hacia orders se añade en V005, cuando existe la tabla de pedidos.
CREATE TABLE stock_reservations (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  public_id   CHAR(26)        NOT NULL,
  order_id    BIGINT UNSIGNED NOT NULL,
  status      VARCHAR(10)     NOT NULL DEFAULT 'ACTIVE',
  expires_at  DATETIME(3)     NOT NULL,
  created_at  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uq_stock_reservations_public_id (public_id),
  UNIQUE KEY uq_stock_reservations_order (order_id),
  KEY idx_stock_reservations_expiry (status, expires_at),
  CONSTRAINT chk_stock_reservations_status CHECK (status IN ('ACTIVE', 'CONFIRMED', 'RELEASED', 'EXPIRED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE stock_reservation_lines (
  reservation_id  BIGINT UNSIGNED NOT NULL,
  product_id      BIGINT UNSIGNED NOT NULL,
  quantity        INT             NOT NULL,
  PRIMARY KEY (reservation_id, product_id),
  KEY idx_reservation_lines_product (product_id),
  CONSTRAINT fk_reservation_lines_reservation FOREIGN KEY (reservation_id) REFERENCES stock_reservations (id) ON DELETE CASCADE,
  CONSTRAINT fk_reservation_lines_product FOREIGN KEY (product_id) REFERENCES products (id),
  CONSTRAINT chk_reservation_lines_quantity CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE stock_movements (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  product_id      BIGINT UNSIGNED NOT NULL,
  movement_type   VARCHAR(16)     NOT NULL,
  quantity        INT             NOT NULL,
  balance_after   INT             NOT NULL,
  reason          VARCHAR(255)    NULL,
  reference_type  VARCHAR(20)     NULL,
  reference_id    BIGINT UNSIGNED NULL,
  created_by      BIGINT UNSIGNED NULL,
  created_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_stock_movements_product (product_id, created_at),
  KEY idx_stock_movements_ref (reference_type, reference_id),
  KEY idx_stock_movements_created_by (created_by),
  CONSTRAINT fk_stock_movements_product FOREIGN KEY (product_id) REFERENCES products (id),
  CONSTRAINT fk_stock_movements_created_by FOREIGN KEY (created_by) REFERENCES users (id),
  CONSTRAINT chk_stock_movements_type CHECK (movement_type IN
    ('SUPPLIER_IN', 'SALE', 'ORDER_CANCEL', 'WASTE', 'ADJUSTMENT', 'RETURN')),
  CONSTRAINT chk_stock_movements_quantity CHECK (quantity <> 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
