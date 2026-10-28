-- V010: inmutabilidad de los registros que no deben modificarse nunca (Fase 5, apartados 3.3 y 7.1).
-- La garantía vive en la propia base de datos (vale para cualquier usuario, también para quien acceda a mano).
-- Son triggers de una sola sentencia, sin bloques BEGIN...END, para que cualquier herramienta los ejecute igual.
-- Las correcciones se hacen añadiendo filas nuevas (facturas rectificativas, movimientos de ajuste, etc.).

CREATE TRIGGER trg_audit_logs_no_update BEFORE UPDATE ON audit_logs
  FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'audit_logs es de solo inserción';
CREATE TRIGGER trg_audit_logs_no_delete BEFORE DELETE ON audit_logs
  FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'audit_logs es de solo inserción';

CREATE TRIGGER trg_payment_events_no_update BEFORE UPDATE ON payment_events
  FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'payment_events es de solo inserción';
CREATE TRIGGER trg_payment_events_no_delete BEFORE DELETE ON payment_events
  FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'payment_events es de solo inserción';

CREATE TRIGGER trg_stock_movements_no_update BEFORE UPDATE ON stock_movements
  FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'stock_movements es de solo inserción';
CREATE TRIGGER trg_stock_movements_no_delete BEFORE DELETE ON stock_movements
  FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'stock_movements es de solo inserción';

CREATE TRIGGER trg_order_status_history_no_update BEFORE UPDATE ON order_status_history
  FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'order_status_history es de solo inserción';
CREATE TRIGGER trg_order_status_history_no_delete BEFORE DELETE ON order_status_history
  FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'order_status_history es de solo inserción';

CREATE TRIGGER trg_product_price_history_no_update BEFORE UPDATE ON product_price_history
  FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'product_price_history es de solo inserción';
CREATE TRIGGER trg_product_price_history_no_delete BEFORE DELETE ON product_price_history
  FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'product_price_history es de solo inserción';

CREATE TRIGGER trg_invoices_no_update BEFORE UPDATE ON invoices
  FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Una factura emitida no se puede modificar: emite una rectificativa';
CREATE TRIGGER trg_invoices_no_delete BEFORE DELETE ON invoices
  FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Una factura emitida no se puede borrar: emite una rectificativa';

CREATE TRIGGER trg_invoice_lines_no_update BEFORE UPDATE ON invoice_lines
  FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Las líneas de una factura emitida no se pueden modificar';
CREATE TRIGGER trg_invoice_lines_no_delete BEFORE DELETE ON invoice_lines
  FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Las líneas de una factura emitida no se pueden borrar';

CREATE TRIGGER trg_invoice_documents_no_update BEFORE UPDATE ON invoice_documents
  FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'invoice_documents es de solo inserción';
CREATE TRIGGER trg_invoice_documents_no_delete BEFORE DELETE ON invoice_documents
  FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'invoice_documents es de solo inserción';
