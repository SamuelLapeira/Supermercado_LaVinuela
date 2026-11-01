-- V103: todas las cuentas de clientes tienen el rol CUSTOMER (historia E1-02).
--
-- Hasta ahora el registro no asignaba ningún rol. A partir de esta versión lo hace al crear la cuenta; esta migración
-- da el rol a las cuentas que se crearon antes y no tienen ninguno.
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
  FROM users u
  JOIN roles r ON r.code = 'CUSTOMER'
 WHERE NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id);
