-- V005: pedidos. El pedido es una instantánea inmutable: guarda los precios, el IVA, el nombre de cada
-- producto, la dirección, la distancia y el plazo tal y como eran al comprar. Si el catálogo o la
-- dirección cambian después, el pedido (y su factura) no cambian.

CREATE TABLE orders (
  id                       BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  public_id                CHAR(26)        NOT NULL,
  order_number             VARCHAR(20)     NOT NULL,
  user_id                  BIGINT UNSIGNED NOT NULL,
  status                   VARCHAR(24)     NOT NULL,
  placed_at                DATETIME(3)     NOT NULL,
  paid_at                  DATETIME(3)     NULL,
  shipped_at               DATETIME(3)     NULL,
  delivered_at             DATETIME(3)     NULL,
  cancelled_at             DATETIME(3)     NULL,
  -- Instantánea de la dirección de entrega.
  source_address_id        BIGINT UNSIGNED NULL,
  ship_recipient_name      VARCHAR(120)    NOT NULL,
  ship_phone               VARCHAR(20)     NULL,
  ship_street              VARCHAR(150)    NOT NULL,
  ship_street_number       VARCHAR(12)     NULL,
  ship_floor_door          VARCHAR(30)     NULL,
  ship_postal_code         CHAR(5)         NOT NULL,
  ship_city                VARCHAR(80)     NOT NULL,
  ship_province            VARCHAR(60)     NOT NULL,
  ship_notes               VARCHAR(255)    NULL,
  ship_latitude            DECIMAL(9,6)    NULL,
  ship_longitude           DECIMAL(9,6)    NULL,
  -- Reparto: distancia por carretera, proveedor que la calculó y plazo estimado (sin franjas horarias).
  delivery_distance_km     DECIMAL(5,1)    NOT NULL,
  distance_provider        VARCHAR(30)     NOT NULL,
  distance_calculated_at   DATETIME(3)     NOT NULL,
  distance_needs_review    BOOLEAN         NOT NULL DEFAULT FALSE,
  estimated_delivery_from  DATE            NOT NULL,
  estimated_delivery_to    DATE            NOT NULL,
  -- Importes: todos con IVA incluido salvo net_total.
  items_total              DECIMAL(10,2)   NOT NULL,
  discount_total           DECIMAL(10,2)   NOT NULL DEFAULT 0,
  shipping_fee             DECIMAL(10,2)   NOT NULL DEFAULT 0,
  total_amount             DECIMAL(10,2)   NOT NULL,
  net_total                DECIMAL(10,2)   NOT NULL,
  tax_total                DECIMAL(10,2)   NOT NULL,
  shipping_tax_rate        DECIMAL(5,2)    NOT NULL DEFAULT 0,
  coupon_code              VARCHAR(40)     NULL,
  age_confirmed            BOOLEAN         NOT NULL DEFAULT FALSE,
  customer_notes           VARCHAR(255)    NULL,
  source                   VARCHAR(10)     NOT NULL DEFAULT 'WEB',
  created_by_staff_id      BIGINT UNSIGNED NULL,
  version                  INT             NOT NULL DEFAULT 0,
  created_at               DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at               DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uq_orders_public_id (public_id),
  UNIQUE KEY uq_orders_number (order_number),
  KEY idx_orders_user (user_id, placed_at DESC),
  KEY idx_orders_queue (status, placed_at),
  KEY idx_orders_delivery (status, estimated_delivery_to),
  KEY idx_orders_placed (placed_at),
  KEY idx_orders_source_address (source_address_id),
  KEY idx_orders_staff (created_by_staff_id),
  CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT fk_orders_source_address FOREIGN KEY (source_address_id) REFERENCES addresses (id),
  CONSTRAINT fk_orders_staff FOREIGN KEY (created_by_staff_id) REFERENCES users (id),
  CONSTRAINT chk_orders_status CHECK (status IN ('PENDING_PAYMENT', 'PAID', 'PREPARING', 'READY_FOR_DELIVERY',
    'OUT_FOR_DELIVERY', 'DELIVERED', 'DELIVERY_FAILED', 'CANCELLED', 'EXPIRED')),
  CONSTRAINT chk_orders_source CHECK (source IN ('WEB', 'MOBILE', 'PHONE')),
  CONSTRAINT chk_orders_dates CHECK (estimated_delivery_from <= estimated_delivery_to),
  CONSTRAINT chk_orders_total CHECK (total_amount = items_total - discount_total + shipping_fee),
  CONSTRAINT chk_orders_amounts CHECK (items_total >= 0 AND discount_total >= 0 AND shipping_fee >= 0 AND total_amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE order_lines (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  order_id        BIGINT UNSIGNED NOT NULL,
  product_id      BIGINT UNSIGNED NOT NULL,   -- referencia; los datos que valen son los de abajo
  sku             VARCHAR(40)     NOT NULL,
  product_name    VARCHAR(160)    NOT NULL,
  format_label    VARCHAR(60)     NOT NULL,
  quantity        INT             NOT NULL,
  unit_price      DECIMAL(10,2)   NOT NULL,
  tax_rate        DECIMAL(5,2)    NOT NULL,
  line_total      DECIMAL(10,2)   NOT NULL,
  line_net        DECIMAL(10,2)   NOT NULL,
  line_tax        DECIMAL(10,2)   NOT NULL,
  age_restricted  BOOLEAN         NOT NULL DEFAULT FALSE,
  line_status     VARCHAR(20)     NOT NULL DEFAULT 'OK',
  created_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uq_order_lines (order_id, product_id),
  KEY idx_order_lines_product (product_id),
  CONSTRAINT fk_order_lines_order FOREIGN KEY (order_id) REFERENCES orders (id),
  CONSTRAINT fk_order_lines_product FOREIGN KEY (product_id) REFERENCES products (id),
  CONSTRAINT chk_order_lines_quantity CHECK (quantity > 0),
  CONSTRAINT chk_order_lines_total CHECK (line_total = unit_price * quantity),
  CONSTRAINT chk_order_lines_status CHECK (line_status IN ('OK', 'MISSING_STORE_ERROR'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Cronología de estados: solo inserción (ver V010).
CREATE TABLE order_status_history (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  order_id     BIGINT UNSIGNED NOT NULL,
  from_status  VARCHAR(24)     NULL,
  to_status    VARCHAR(24)     NOT NULL,
  changed_by   BIGINT UNSIGNED NULL,
  reason       VARCHAR(255)    NULL,
  created_at   DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at   DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_order_status_history_order (order_id, created_at),
  KEY idx_order_status_history_changed_by (changed_by),
  CONSTRAINT fk_order_status_history_order FOREIGN KEY (order_id) REFERENCES orders (id),
  CONSTRAINT fk_order_status_history_changed_by FOREIGN KEY (changed_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Faltas por error de la tienda. El reembolso se vincula desde la tabla refunds (V006).
CREATE TABLE order_incidents (
  id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  public_id        CHAR(26)        NOT NULL,
  order_id         BIGINT UNSIGNED NOT NULL,
  order_line_id    BIGINT UNSIGNED NULL,
  incident_type    VARCHAR(20)     NOT NULL,
  description      VARCHAR(500)    NOT NULL,
  refund_amount    DECIMAL(10,2)   NULL,
  registered_by    BIGINT UNSIGNED NOT NULL,
  created_at       DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at       DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uq_order_incidents_public_id (public_id),
  KEY idx_order_incidents_order (order_id),
  KEY idx_order_incidents_line (order_line_id),
  KEY idx_order_incidents_registered_by (registered_by),
  CONSTRAINT fk_order_incidents_order FOREIGN KEY (order_id) REFERENCES orders (id),
  CONSTRAINT fk_order_incidents_line FOREIGN KEY (order_line_id) REFERENCES order_lines (id),
  CONSTRAINT fk_order_incidents_registered_by FOREIGN KEY (registered_by) REFERENCES users (id),
  CONSTRAINT chk_order_incidents_type CHECK (incident_type IN ('MISSING_ITEM', 'DAMAGED_ITEM', 'WRONG_ITEM', 'OTHER')),
  CONSTRAINT chk_order_incidents_refund CHECK (refund_amount IS NULL OR refund_amount > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Numeración legible de pedidos (LV-2026-000123): la aplicación bloquea la fila para incrementar.
CREATE TABLE order_sequences (
  seq_year     SMALLINT NOT NULL,
  last_number  INT      NOT NULL DEFAULT 0,
  PRIMARY KEY (seq_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE coupon_redemptions (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  coupon_id   BIGINT UNSIGNED NOT NULL,
  user_id     BIGINT UNSIGNED NOT NULL,
  order_id    BIGINT UNSIGNED NOT NULL,
  amount      DECIMAL(10,2)   NOT NULL,
  created_at  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uq_coupon_redemptions_order (order_id),
  KEY idx_coupon_redemptions (coupon_id, user_id),
  KEY idx_coupon_redemptions_user (user_id),
  CONSTRAINT fk_coupon_redemptions_coupon FOREIGN KEY (coupon_id) REFERENCES coupons (id),
  CONSTRAINT fk_coupon_redemptions_user FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT fk_coupon_redemptions_order FOREIGN KEY (order_id) REFERENCES orders (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Ahora que existe orders, la reserva de stock queda ligada a su pedido.
ALTER TABLE stock_reservations
  ADD CONSTRAINT fk_stock_reservations_order FOREIGN KEY (order_id) REFERENCES orders (id);
