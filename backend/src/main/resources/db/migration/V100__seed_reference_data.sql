-- V100: datos de referencia imprescindibles para arrancar. Nada de datos de ejemplo (eso va en un perfil aparte)
-- y ninguna contraseña: el usuario administrador inicial se crea con una orden de arranque (historia E1-05).

-- ---------------------------------------------------------------- roles y permisos (Fase 3, apartado 9.3)
INSERT INTO roles (code, name) VALUES
  ('CUSTOMER',        'Cliente'),
  ('PICKER',          'Preparador de pedidos'),
  ('DRIVER',          'Repartidor'),
  ('CATALOG_MANAGER', 'Encargado de catálogo'),
  ('ADMIN',           'Administrador');

INSERT INTO permissions (code, description) VALUES
  ('ORDERS_READ',       'Ver pedidos y su preparación'),
  ('ORDERS_PROCESS',    'Preparar, despachar y entregar pedidos'),
  ('ORDERS_REFUND',     'Registrar incidencias y hacer reembolsos'),
  ('CATALOG_WRITE',     'Crear y editar productos, categorías y marcas'),
  ('STOCK_WRITE',       'Registrar entradas, ajustes y mermas de stock'),
  ('PROMOTIONS_WRITE',  'Gestionar cupones y promociones'),
  ('CONTENT_WRITE',     'Editar banner, páginas y plantillas de correo'),
  ('DELIVERY_CONFIG',   'Configurar distancia máxima, tarifas y plazos de reparto'),
  ('SETTINGS_WRITE',    'Cambiar la marca de la tienda (nombre, logo, color) y otros ajustes'),
  ('CUSTOMERS_READ',    'Consultar clientes'),
  ('BILLING_MANAGE',    'Gestionar facturas y notas de abono'),
  ('REPORTS_READ',      'Consultar informes de ventas y stock'),
  ('REPORTS_STOCK_READ', 'Consultar solo informes de stock'),
  ('STAFF_MANAGE',      'Gestionar usuarios internos y sus permisos'),
  ('AUDIT_READ',        'Consultar el registro de auditoría');

-- Preparador y repartidor: ver y procesar pedidos.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p
 WHERE r.code IN ('PICKER', 'DRIVER') AND p.code IN ('ORDERS_READ', 'ORDERS_PROCESS');

-- Encargado de catálogo.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p
 WHERE r.code = 'CATALOG_MANAGER'
   AND p.code IN ('CATALOG_WRITE', 'STOCK_WRITE', 'PROMOTIONS_WRITE', 'CONTENT_WRITE', 'REPORTS_STOCK_READ');

-- Administrador: todos los permisos.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p WHERE r.code = 'ADMIN';

-- ---------------------------------------------------------------- IVA (se confirman con el asesor al lanzar)
INSERT INTO tax_rates (code, name, rate, valid_from) VALUES
  ('VAT_SUPER_REDUCED', 'IVA superreducido', 4.00,  '2000-01-01'),
  ('VAT_REDUCED',       'IVA reducido',      10.00, '2000-01-01'),
  ('VAT_GENERAL',       'IVA general',       21.00, '2000-01-01');

-- ---------------------------------------------------------------- alérgenos de declaración obligatoria (UE)
INSERT INTO allergens (code, name) VALUES
  ('gluten',          'Cereales con gluten'),
  ('crustaceans',     'Crustáceos'),
  ('eggs',            'Huevos'),
  ('fish',            'Pescado'),
  ('peanuts',         'Cacahuetes'),
  ('soy',             'Soja'),
  ('milk',            'Leche y lácteos (incluida la lactosa)'),
  ('tree_nuts',       'Frutos de cáscara'),
  ('celery',          'Apio'),
  ('mustard',         'Mostaza'),
  ('sesame',          'Granos de sésamo'),
  ('sulphites',       'Dióxido de azufre y sulfitos'),
  ('lupin',           'Altramuces'),
  ('molluscs',        'Moluscos');

-- ---------------------------------------------------------------- tramos de tarifa de reparto (ejemplo editable)
INSERT INTO delivery_fee_bands (from_km, to_km, fee) VALUES
  (0.0,  10.0, 2.50),
  (10.0, 20.0, 3.50),
  (20.0, 30.0, 5.50),
  (30.0, 40.0, 7.50);

-- ---------------------------------------------------------------- ajustes (los edita la empresa en el panel)
INSERT INTO app_settings (setting_key, setting_value, value_type, scope) VALUES
  ('delivery.max_km',              '40',   'NUMBER',  'SYSTEM'),
  ('delivery.min_order',           '20',   'NUMBER',  'SYSTEM'),
  ('delivery.free_shipping_from',  '60',   'NUMBER',  'SYSTEM'),
  ('delivery.eta_min_days',        '1',    'NUMBER',  'SYSTEM'),
  ('delivery.eta_max_days',        '3',    'NUMBER',  'SYSTEM'),
  ('checkout.reservation_minutes', '15',   'NUMBER',  'SYSTEM'),
  ('routing.fallback_factor',      '1.5',  'NUMBER',  'SYSTEM'),
  ('store.address',                'C. Pizarras, 34, 29712 La Viñuela, Málaga', 'STRING', 'SYSTEM'),
  -- Coordenadas aproximadas del municipio: hay que sustituirlas por las exactas de la tienda.
  ('store.latitude',               '36.850',  'NUMBER', 'SYSTEM'),
  ('store.longitude',              '-4.133',  'NUMBER', 'SYSTEM'),
  ('site.name',                    'La Viñuela', 'STRING', 'SITE'),
  ('site.tagline',                 'Supermercado de la sierra', 'STRING', 'SITE'),
  ('site.brand_color',             '#0F5E63', 'COLOR',  'SITE'),
  ('site.logo_media',              NULL,      'MEDIA',  'SITE'),
  ('site.announcement',            'Repartimos por toda la comarca. Tu pedido llega en 1-3 días.', 'STRING', 'SITE'),
  ('site.opening_hours',           'Lunes a sábado 9:00-21:00. Domingos y festivos 9:30-14:30.', 'STRING', 'SITE'),
  ('site.phone',                   NULL,      'STRING', 'SITE');

-- ---------------------------------------------------------------- contenido inicial
INSERT INTO content_blocks (block_key, status, data, published_at) VALUES
  ('home.banner', 'PUBLISHED',
   JSON_OBJECT(
     'enabled',  TRUE,
     'title',    'Tu compra, en casa, sin salir de la sierra',
     'subtitle', 'Producto fresco, de aquí y de siempre. Hacemos el pedido y te lo llevamos.',
     'ctaLabel', 'Ver ofertas de la semana',
     'mediaId',  NULL),
   CURRENT_TIMESTAMP(3));

-- Textos legales: son BORRADORES vacíos. Los debe redactar o revisar un asesor antes de publicarse.
INSERT INTO content_pages (slug, title, body_html, status, show_in_footer, position) VALUES
  ('aviso-legal',  'Aviso legal',                 '<p>BORRADOR: este texto debe redactarlo o revisarlo un asesor antes de publicarse.</p>', 'DRAFT', TRUE, 10),
  ('privacidad',   'Política de privacidad',      '<p>BORRADOR: este texto debe redactarlo o revisarlo un asesor antes de publicarse.</p>', 'DRAFT', TRUE, 20),
  ('cookies',      'Política de cookies',         '<p>BORRADOR: este texto debe redactarlo o revisarlo un asesor antes de publicarse.</p>', 'DRAFT', TRUE, 30),
  ('condiciones',  'Condiciones de compra',       '<p>BORRADOR: este texto debe redactarlo o revisarlo un asesor antes de publicarse.</p>', 'DRAFT', TRUE, 40),
  ('contacto',     'Contacto',                    '<p>BORRADOR: añade aquí la dirección, el teléfono y el horario de la tienda.</p>',        'DRAFT', TRUE, 50);

-- Plantillas de correo. Marcadores permitidos entre llaves dobles: se sustituyen al enviar.
INSERT INTO email_templates (code, locale, subject, body_html, body_text) VALUES
  ('EMAIL_VERIFICATION', 'es-ES',
   'Confirma tu correo en {{siteName}}',
   '<p>Hola, {{customerName}}:</p><p>Para activar tu cuenta, confirma tu correo en este enlace: <a href="{{link}}">{{link}}</a></p><p>El enlace caduca en 24 horas. Si no has sido tú, ignora este mensaje.</p>',
   'Hola, {{customerName}}. Para activar tu cuenta, confirma tu correo en este enlace: {{link}} (caduca en 24 horas). Si no has sido tú, ignora este mensaje.'),
  ('PASSWORD_RESET', 'es-ES',
   'Cambia tu contraseña de {{siteName}}',
   '<p>Hola, {{customerName}}:</p><p>Puedes elegir una contraseña nueva en este enlace: <a href="{{link}}">{{link}}</a></p><p>El enlace caduca en 1 hora y solo se puede usar una vez. Si no lo has pedido tú, ignora este mensaje.</p>',
   'Hola, {{customerName}}. Elige una contraseña nueva en este enlace: {{link}} (caduca en 1 hora y solo se puede usar una vez). Si no lo has pedido tú, ignora este mensaje.'),
  ('ORDER_CONFIRMATION', 'es-ES',
   'Hemos recibido tu pedido {{orderNumber}}',
   '<p>Hola, {{customerName}}:</p><p>Hemos recibido tu pedido <strong>{{orderNumber}}</strong> por un total de {{total}}.</p><p>Te llegará {{eta}}. Te avisaremos por correo cuando salga de la tienda.</p><p>Gracias por comprar en {{siteName}}.</p>',
   'Hola, {{customerName}}. Hemos recibido tu pedido {{orderNumber}} por un total de {{total}}. Te llegará {{eta}}. Te avisaremos cuando salga de la tienda. Gracias por comprar en {{siteName}}.'),
  ('ORDER_DISPATCHED', 'es-ES',
   'Tu pedido {{orderNumber}} ha salido de la tienda',
   '<p>Hola, {{customerName}}:</p><p>Tu pedido <strong>{{orderNumber}}</strong> ya está en reparto. Llegará {{eta}}.</p><p>Si necesitas algo, llámanos al {{phone}}.</p>',
   'Hola, {{customerName}}. Tu pedido {{orderNumber}} ya está en reparto y llegará {{eta}}. Si necesitas algo, llámanos al {{phone}}.'),
  ('ORDER_DELIVERED', 'es-ES',
   'Pedido {{orderNumber}} entregado',
   '<p>Hola, {{customerName}}:</p><p>Tu pedido <strong>{{orderNumber}}</strong> se ha entregado. Adjuntamos tu ticket.</p><p>Gracias por confiar en {{siteName}}.</p>',
   'Hola, {{customerName}}. Tu pedido {{orderNumber}} se ha entregado. Adjuntamos tu ticket. Gracias por confiar en {{siteName}}.'),
  ('REFUND_ISSUED', 'es-ES',
   'Te hemos devuelto {{amount}} de tu pedido {{orderNumber}}',
   '<p>Hola, {{customerName}}:</p><p>Lamentamos el error. Te hemos devuelto <strong>{{amount}}</strong> del pedido <strong>{{orderNumber}}</strong>. El banco puede tardar unos días en mostrarlo.</p>',
   'Hola, {{customerName}}. Lamentamos el error. Te hemos devuelto {{amount}} del pedido {{orderNumber}}. El banco puede tardar unos días en mostrarlo.');
