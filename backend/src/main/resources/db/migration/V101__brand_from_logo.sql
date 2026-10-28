-- V101: el color principal por defecto pasa a ser el verde del logotipo de La Viñuela (#007924), que cumple
-- el contraste AA con texto blanco. La empresa puede cambiarlo desde el panel (historia E10-01).
-- Es una migración nueva y no un cambio de V100, porque una migración ya aplicada no se edita nunca.
UPDATE app_settings
   SET setting_value = '#007924'
 WHERE setting_key = 'site.brand_color';
