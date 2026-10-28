-- V007: contenido editable por la empresa, plantillas de correo, importaciones, notificaciones y auditoría.

-- Bloques de contenido (banner de portada...). Como mucho un borrador y una versión publicada por bloque.
CREATE TABLE content_blocks (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  block_key     VARCHAR(60)     NOT NULL,
  status        VARCHAR(10)     NOT NULL,
  data          JSON            NOT NULL,
  published_at  DATETIME(3)     NULL,
  version       INT             NOT NULL DEFAULT 0,
  created_by    BIGINT UNSIGNED NULL,
  updated_by    BIGINT UNSIGNED NULL,
  created_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uq_content_blocks (block_key, status),
  KEY idx_content_blocks_created_by (created_by),
  KEY idx_content_blocks_updated_by (updated_by),
  CONSTRAINT fk_content_blocks_created_by FOREIGN KEY (created_by) REFERENCES users (id),
  CONSTRAINT fk_content_blocks_updated_by FOREIGN KEY (updated_by) REFERENCES users (id),
  CONSTRAINT chk_content_blocks_status CHECK (status IN ('DRAFT', 'PUBLISHED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Páginas informativas y legales. El texto se guarda ya saneado (lista blanca de etiquetas).
CREATE TABLE content_pages (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  slug            VARCHAR(80)     NOT NULL,
  title           VARCHAR(160)    NOT NULL,
  body_html       MEDIUMTEXT      NOT NULL,
  status          VARCHAR(10)     NOT NULL,
  show_in_footer  BOOLEAN         NOT NULL DEFAULT FALSE,
  position        INT             NOT NULL DEFAULT 0,
  published_at    DATETIME(3)     NULL,
  version         INT             NOT NULL DEFAULT 0,
  created_by      BIGINT UNSIGNED NULL,
  updated_by      BIGINT UNSIGNED NULL,
  created_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uq_content_pages_slug (slug),
  KEY idx_content_pages_created_by (created_by),
  KEY idx_content_pages_updated_by (updated_by),
  CONSTRAINT fk_content_pages_created_by FOREIGN KEY (created_by) REFERENCES users (id),
  CONSTRAINT fk_content_pages_updated_by FOREIGN KEY (updated_by) REFERENCES users (id),
  CONSTRAINT chk_content_pages_status CHECK (status IN ('DRAFT', 'PUBLISHED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE email_templates (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  code        VARCHAR(40)     NOT NULL,
  locale      CHAR(5)         NOT NULL DEFAULT 'es-ES',
  subject     VARCHAR(200)    NOT NULL,
  body_html   MEDIUMTEXT      NOT NULL,
  body_text   TEXT            NULL,
  active      BOOLEAN         NOT NULL DEFAULT TRUE,
  version     INT             NOT NULL DEFAULT 0,
  created_by  BIGINT UNSIGNED NULL,
  updated_by  BIGINT UNSIGNED NULL,
  created_at  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uq_email_templates (code, locale),
  KEY idx_email_templates_created_by (created_by),
  KEY idx_email_templates_updated_by (updated_by),
  CONSTRAINT fk_email_templates_created_by FOREIGN KEY (created_by) REFERENCES users (id),
  CONSTRAINT fk_email_templates_updated_by FOREIGN KEY (updated_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE import_jobs (
  id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  public_id           CHAR(26)        NOT NULL,
  job_type            VARCHAR(20)     NOT NULL,
  source_storage_key  VARCHAR(255)    NOT NULL,
  status              VARCHAR(12)     NOT NULL,
  total_rows          INT             NULL,
  ok_rows             INT             NULL,
  error_rows          INT             NULL,
  report_storage_key  VARCHAR(255)    NULL,
  created_by          BIGINT UNSIGNED NOT NULL,
  created_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uq_import_jobs_public_id (public_id),
  KEY idx_import_jobs_created_by (created_by, created_at),
  CONSTRAINT fk_import_jobs_created_by FOREIGN KEY (created_by) REFERENCES users (id),
  CONSTRAINT chk_import_jobs_status CHECK (status IN ('UPLOADED', 'VALIDATED', 'APPLYING', 'DONE', 'FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE notifications (
  id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id          BIGINT UNSIGNED NULL,
  recipient_email  VARCHAR(254)    NOT NULL,
  template_code    VARCHAR(40)     NOT NULL,
  status           VARCHAR(8)      NOT NULL DEFAULT 'PENDING',
  attempts         SMALLINT        NOT NULL DEFAULT 0,
  last_error       VARCHAR(255)    NULL,
  reference_type   VARCHAR(20)     NULL,
  reference_id     BIGINT UNSIGNED NULL,
  sent_at          DATETIME(3)     NULL,
  created_at       DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at       DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_notifications_pending (status, created_at),
  KEY idx_notifications_user (user_id),
  CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT chk_notifications_status CHECK (status IN ('PENDING', 'SENT', 'FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Auditoría: solo inserción (ver V010). Sin claves foráneas: el registro debe sobrevivir aunque se
-- anonimice o desactive a quien hizo la acción.
CREATE TABLE audit_logs (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  created_at     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  actor_user_id  BIGINT UNSIGNED NULL,
  actor_role     VARCHAR(30)     NULL,
  action         VARCHAR(60)     NOT NULL,
  entity_type    VARCHAR(40)     NOT NULL,
  entity_id      VARCHAR(40)     NULL,
  before_data    JSON            NULL,
  after_data     JSON            NULL,
  ip             VARBINARY(16)   NULL,
  user_agent     VARCHAR(255)    NULL,
  trace_id       VARCHAR(40)     NULL,
  PRIMARY KEY (id),
  KEY idx_audit_logs_entity (entity_type, entity_id, created_at),
  KEY idx_audit_logs_actor (actor_user_id, created_at),
  KEY idx_audit_logs_action (action, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
