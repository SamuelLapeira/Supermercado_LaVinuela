-- V008: tablas de sistema (idempotencia, bloqueo de tareas programadas) y resúmenes para informes.
-- La tabla de eventos de Spring Modulith (event_publication) NO se crea aquí: se añade con la historia que
-- incorpore el registro de eventos, copiando el esquema exacto de la versión de Modulith en uso.

CREATE TABLE idempotency_keys (
  key_hash         CHAR(64)    NOT NULL,
  scope            VARCHAR(40) NOT NULL,
  request_hash     CHAR(64)    NOT NULL,
  response_status  SMALLINT    NOT NULL,
  response_body    JSON        NULL,
  created_at       DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  expires_at       DATETIME(3) NOT NULL,
  PRIMARY KEY (key_hash, scope),
  KEY idx_idempotency_keys_expiry (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Estructura estándar de ShedLock: una sola instancia ejecuta cada tarea programada.
CREATE TABLE shedlock (
  name        VARCHAR(64)  NOT NULL,
  lock_until  TIMESTAMP(3) NOT NULL,
  locked_at   TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  locked_by   VARCHAR(255) NOT NULL,
  PRIMARY KEY (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE daily_sales_summary (
  summary_date    DATE           NOT NULL,
  orders_count    INT            NOT NULL,
  items_count     INT            NOT NULL,
  gross_total     DECIMAL(12,2)  NOT NULL,
  net_total       DECIMAL(12,2)  NOT NULL,
  tax_total       DECIMAL(12,2)  NOT NULL,
  shipping_total  DECIMAL(12,2)  NOT NULL,
  refunds_total   DECIMAL(12,2)  NOT NULL,
  PRIMARY KEY (summary_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE product_sales_daily (
  summary_date  DATE            NOT NULL,
  product_id    BIGINT UNSIGNED NOT NULL,
  units         INT             NOT NULL,
  revenue       DECIMAL(12,2)   NOT NULL,
  PRIMARY KEY (summary_date, product_id),
  KEY idx_product_sales_daily_product (product_id, summary_date),
  CONSTRAINT fk_product_sales_daily_product FOREIGN KEY (product_id) REFERENCES products (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
