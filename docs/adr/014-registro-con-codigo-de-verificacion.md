# ADR-014: Registro con usuario, contraseña y código de verificación por correo

**Estado:** Aceptada

## Contexto

La empresa quiere un registro rápido: nombre de usuario, correo y contraseña, con confirmación del correo. El
teléfono y las direcciones se piden en el primer pedido. El diseño original (Fases 3 y 5) pensaba en un enlace de
verificación y en nombre, apellidos y teléfono obligatorios al registrarse.

## Decisión

- **Código de 6 cifras** en lugar de enlace: 15 minutos de validez, 5 intentos fallidos, espera de 60 segundos entre
  envíos y máximo 5 por hora. Un código nuevo anula los anteriores.
- **Se guarda solo una huella** del código: HMAC-SHA256 con una clave secreta del servidor (`APP_CODE_PEPPER`) y el
  identificador de la cuenta. El código en claro viaja únicamente en un evento en memoria que se entrega **después de
  confirmar** la transacción; no se escribe en ninguna tabla.
- **No se revela qué correos tienen cuenta**: registrarse con un correo existente da la misma respuesta (código nuevo si
  estaba pendiente, aviso por correo a su titular si ya estaba activa). Los nombres de usuario sí pueden estar
  «cogidos», como en cualquier servicio con nombres de usuario.
- **Contraseñas con Argon2id** (formato estándar, sal propia, 19 MiB / 2 iteraciones / 1 hilo), con la implementación
  de Bouncy Castle (1.85 o posterior por una vulnerabilidad crítica en versiones anteriores). **Política de
  contraseñas:** mínimo de 10 caracteres con al menos una mayúscula, una minúscula y un número (decisión de la
  empresa), más una lista de contraseñas comunes, rechazo de repeticiones y de las que contienen el usuario o el
  correo. Las recomendaciones del NIST 800-63B priorizan la longitud sobre las reglas de composición; si más adelante
  se quiere endurecer, lo más eficaz es subir el mínimo a 12 caracteres.
- **Migración V102:** `users.name` y `users.surname` pasan a ser opcionales, se añade `users.username` (único, sin
  distinguir mayúsculas) y `user_tokens.failed_attempts`; las plantillas de correo pasan de enlace a código y se añade
  el aviso «ya tienes una cuenta».
- **Evento entre módulos:** identidad publica `IdentityEvents.VerificationCodeIssued` y `AccountAlreadyExists`;
  notificaciones los escucha y envía los correos con las plantillas que edita la empresa (módulo de contenido).
  El módulo de identidad no depende del de notificaciones.
- **Mapeos a mano** entre el dominio y las entidades JPA por ahora; MapStruct (previsto en la Fase 3) se incorporará
  cuando haya suficientes mapeos que lo justifiquen.

## Consecuencias

- Con el registro mínimo, los datos de la persona (nombre real, teléfono, direcciones) deberán pedirse en el primer
  pedido, y cada pedido guarda su propia copia de los datos de entrega.
- Quien reciba un correo con un código que no pidió no corre riesgo: sin el código nadie usa su correo.
- Las pruebas del flujo completo necesitan Docker (MySQL real); la lógica se prueba también sin base de datos con
  repositorios en memoria.
- Detalle del recorrido, la API y la seguridad en `docs/identity.md`.
