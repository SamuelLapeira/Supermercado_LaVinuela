-- V102: registro con nombre de usuario, contraseña y código de verificación por correo (historia E1-01).
--
-- 1) El nombre y los apellidos reales, y el teléfono, se piden en el primer pedido, no al registrarse: pasan a ser
--    opcionales en la cuenta. (Cada pedido guarda su propia copia de los datos de entrega, así que no se pierde nada.)
-- 2) Nueva columna con el nombre de usuario. Es única sin distinguir mayúsculas ni acentos (el orden de
--    comparación de la tabla), y se deja opcional porque las cuentas creadas desde el panel pueden no tenerlo.
-- 3) Los códigos de verificación cuentan sus intentos fallidos: con 5 hay que pedir uno nuevo.
-- 4) Las plantillas de correo pasan de «enlace» a «código», y se añade el aviso de «ya tienes una cuenta».

ALTER TABLE users
  MODIFY COLUMN name    VARCHAR(80)  NULL,
  MODIFY COLUMN surname VARCHAR(120) NULL,
  ADD COLUMN username VARCHAR(30) NULL AFTER email,
  ADD UNIQUE KEY uq_users_username (username);

ALTER TABLE user_tokens
  ADD COLUMN failed_attempts SMALLINT UNSIGNED NOT NULL DEFAULT 0 AFTER used_at;

UPDATE email_templates
   SET subject   = 'Tu código de verificación de {{siteName}}',
       body_html = '<p>Hola, {{customerName}}:</p><p>Tu código de verificación es:</p><p style="font-size:28px;font-weight:bold;letter-spacing:6px;margin:16px 0">{{code}}</p><p>Escríbelo en la página para activar tu cuenta. Caduca en {{minutes}} minutos.</p><p>Si no has sido tú, ignora este mensaje: nadie podrá usar tu correo sin este código.</p>',
       body_text = 'Hola, {{customerName}}. Tu código de verificación de {{siteName}} es {{code}}. Escríbelo en la página para activar tu cuenta. Caduca en {{minutes}} minutos. Si no has sido tú, ignora este mensaje.'
 WHERE code = 'EMAIL_VERIFICATION' AND locale = 'es-ES';

INSERT INTO email_templates (code, locale, subject, body_html, body_text) VALUES
  ('ACCOUNT_EXISTS', 'es-ES',
   'Ya tienes una cuenta en {{siteName}}',
   '<p>Hola, {{customerName}}:</p><p>Alguien ha intentado crear una cuenta en {{siteName}} con este correo, pero ya tienes una.</p><p>Si has sido tú, inicia sesión con tu usuario y tu contraseña. Si no la recuerdas, puedes cambiarla desde la página de inicio de sesión.</p><p>Si no has sido tú, no tienes que hacer nada.</p>',
   'Hola, {{customerName}}. Alguien ha intentado crear una cuenta en {{siteName}} con este correo, pero ya tienes una. Si has sido tú, inicia sesión con tu usuario y tu contraseña (si no la recuerdas, puedes cambiarla desde la página de inicio de sesión). Si no has sido tú, no tienes que hacer nada.');
