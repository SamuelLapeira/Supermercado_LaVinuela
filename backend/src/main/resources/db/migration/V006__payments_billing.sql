-- V006: pagos (Redsys), reembolsos y facturación.

CREATE TABLE payments (
  id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  public_id           CHAR(26)        NOT NULL,
  order_id            BIGINT UNSIGNED NOT NULL,
  provider            VARCHAR(20)     NOT NULL DEFAULT 'REDSYS',
  -- Referencia de la operación en Redsys: 12 caracteres, los 4 primeros numéricos. Una por intento de pago.
  redsys_order_ref    CHAR(12)        NOT NULL,
  amount              DECIMAL(10,2)   NOT NULL,
  currency            CHAR(3)         NOT NULL DEFAULT 'EUR',
  status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
  authorization_code  VARCHAR(12)     NULL,
  response_code       VARCHAR(4)      NULL,
  idempotency_key     VARCHAR(64)     NULL,
  paid_at             DATETIME(3)     NULL,
  -- Un pedido no puede tener dos pagos cobrados: columna calculada + índice único.
  paid_order_id       BIGINT UNSIGNED GENERATED ALWAYS AS
                        (IF(status IN ('PAID', 'PARTIALLY_REFUNDED', 'REFUNDED'), order_id, NULL)) VIRTUAL,
  version             INT             NOT NULL DEFAULT 0,
  created_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uq_payments_public_id (public_id),
  UNIQUE KEY uq_payments_redsys_ref (redsys_order_ref),
  UNIQUE KEY uq_payments_idempotency (idempotency_key),
  UNIQUE KEY uq_payments_one_paid (paid_order_id),
  KEY idx_payments_order (order_id),
  KEY idx_payments_status (status, created_at),
  CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders (id),
  CONSTRAINT chk_payments_amount CHECK (amount > 0),
  CONSTRAINT chk_payments_status CHECK (status IN
    ('PENDING', 'PAID', 'FAILED', 'EXPIRED', 'PARTIALLY_REFUNDED', 'REFUNDED')),
  CONSTRAINT chk_payments_redsys_ref CHECK (redsys_order_ref REGEXP '^[0-9]{4}[0-9A-Za-z]{8}$')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Todo lo que llega de Redsys, sea válido o no. Solo inserción (ver V010).
CREATE TABLE payment_events (
  id                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  payment_id         BIGINT UNSIGNED NULL,
  event_source       VARCHAR(10)     NOT NULL,
  signature_valid    BOOLEAN         NOT NULL,
  raw_payload        TEXT            NOT NULL,
  raw_signature      VARCHAR(255)    NULL,
  processing_result  VARCHAR(30)     NOT NULL,
  created_at         DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at         DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_payment_events_payment (payment_id),
  KEY idx_payment_events_date (created_at),
  CONSTRAINT fk_payment_events_payment FOREIGN KEY (payment_id) REFERENCES payments (id),
  CONSTRAINT chk_payment_events_source CHECK (event_source IN ('NOTIFY', 'RETURN', 'RECONCILE', 'REFUND'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE refunds (
  id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  public_id        CHAR(26)        NOT NULL,
  payment_id       BIGINT UNSIGNED NOT NULL,
  order_id         BIGINT UNSIGNED NOT NULL,
  order_line_id    BIGINT UNSIGNED NULL,
  incident_id      BIGINT UNSIGNED NULL,
  amount           DECIMAL(10,2)   NOT NULL,
  reason           VARCHAR(24)     NOT NULL,
  reason_detail    VARCHAR(255)    NOT NULL,
  status           VARCHAR(8)      NOT NULL DEFAULT 'PENDING',
  provider_ref     VARCHAR(40)     NULL,
  idempotency_key  VARCHAR(64)     NOT NULL,
  created_by       BIGINT UNSIGNED NULL,
  processed_at     DATETIME(3)     NULL,
  created_at       DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at       DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uq_refunds_public_id (public_id),
  UNIQUE KEY uq_refunds_idempotency (idempotency_key),
  KEY idx_refunds_payment (payment_id),
  KEY idx_refunds_order (order_id),
  KEY idx_refunds_line (order_line_id),
  KEY idx_refunds_incident (incident_id),
  KEY idx_refunds_created_by (created_by),
  KEY idx_refunds_status (status, created_at),
  CONSTRAINT fk_refunds_payment FOREIGN KEY (payment_id) REFERENCES payments (id),
  CONSTRAINT fk_refunds_order FOREIGN KEY (order_id) REFERENCES orders (id),
  CONSTRAINT fk_refunds_line FOREIGN KEY (order_line_id) REFERENCES order_lines (id),
  CONSTRAINT fk_refunds_incident FOREIGN KEY (incident_id) REFERENCES order_incidents (id),
  CONSTRAINT fk_refunds_created_by FOREIGN KEY (created_by) REFERENCES users (id),
  CONSTRAINT chk_refunds_amount CHECK (amount > 0),
  CONSTRAINT chk_refunds_reason CHECK (reason IN
    ('STORE_ERROR', 'CANCELLED_BY_CUSTOMER', 'LATE_PAYMENT', 'DUPLICATE_PAYMENT', 'OTHER')),
  CONSTRAINT chk_refunds_status CHECK (status IN ('PENDING', 'DONE', 'FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Numeración correlativa y sin huecos de facturas: la aplicación bloquea la fila para incrementar.
CREATE TABLE invoice_sequences (
  series       VARCHAR(8) NOT NULL,
  fiscal_year  SMALLINT   NOT NULL,
  last_number  INT        NOT NULL DEFAULT 0,
  PRIMARY KEY (series, fiscal_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Facturas y tickets: inmutables una vez emitidos (ver V010). Las correcciones se hacen con rectificativas.
CREATE TABLE invoices (
  id                    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  public_id             CHAR(26)        NOT NULL,
  order_id              BIGINT UNSIGNED NOT NULL,
  invoice_type          VARCHAR(12)     NOT NULL,
  series                VARCHAR(8)      NOT NULL,
  fiscal_year           SMALLINT        NOT NULL,
  number                INT             NOT NULL,
  full_number           VARCHAR(24)     NOT NULL,
  issue_date            DATE            NOT NULL,
  rectifies_invoice_id  BIGINT UNSIGNED NULL,
  customer_tax_id       VARCHAR(20)     NULL,
  customer_name         VARCHAR(160)    NULL,
  customer_address      VARCHAR(255)    NULL,
  net_total             DECIMAL(10,2)   NOT NULL,
  tax_total             DECIMAL(10,2)   NOT NULL,
  total                 DECIMAL(10,2)   NOT NULL,
  created_at            DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at            DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uq_invoices_public_id (public_id),
  UNIQUE KEY uq_invoices_full_number (full_number),
  UNIQUE KEY uq_invoices_number (series, fiscal_year, number),
  KEY idx_invoices_order (order_id),
  KEY idx_invoices_date (issue_date),
  KEY idx_invoices_rectifies (rectifies_invoice_id),
  CONSTRAINT fk_invoices_order FOREIGN KEY (order_id) REFERENCES orders (id),
  CONSTRAINT fk_invoices_rectifies FOREIGN KEY (rectifies_invoice_id) REFERENCES invoices (id),
  CONSTRAINT chk_invoices_type CHECK (invoice_type IN ('SIMPLIFIED', 'FULL', 'CREDIT_NOTE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE invoice_lines (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  invoice_id   BIGINT UNSIGNED NOT NULL,
  line_no      SMALLINT        NOT NULL,
  description  VARCHAR(200)    NOT NULL,
  quantity     INT             NOT NULL,
  unit_price   DECIMAL(10,2)   NOT NULL,
  tax_rate     DECIMAL(5,2)    NOT NULL,
  net          DECIMAL(10,2)   NOT NULL,
  tax          DECIMAL(10,2)   NOT NULL,
  total        DECIMAL(10,2)   NOT NULL,
  created_at   DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at   DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uq_invoice_lines (invoice_id, line_no),
  CONSTRAINT fk_invoice_lines_invoice FOREIGN KEY (invoice_id) REFERENCES invoices (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- El PDF se guarda aparte (y solo se inserta): así la factura emitida no se modifica nunca.
-- Sustituye al campo pdf_storage_key que preveía la Fase 5 dentro de invoices.
CREATE TABLE invoice_documents (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  invoice_id   BIGINT UNSIGNED NOT NULL,
  storage_key  VARCHAR(255)    NOT NULL,
  created_at   DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at   DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uq_invoice_documents_invoice (invoice_id),
  CONSTRAINT fk_invoice_documents_invoice FOREIGN KEY (invoice_id) REFERENCES invoices (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
