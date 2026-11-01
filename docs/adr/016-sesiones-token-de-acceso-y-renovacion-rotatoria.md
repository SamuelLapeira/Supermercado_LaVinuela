# ADR-016: Sesiones con token de acceso corto y token de renovación rotatorio

**Estado:** Aceptada (concreta la decisión ADR-005)

## Contexto

La tienda web y, más adelante, una posible aplicación móvil necesitan una sesión segura. Los datos de las personas y los
pedidos dependen de que nadie pueda hacerse pasar por otra persona.

## Decisión

- **Token de acceso: JWT firmado (HS256) de 15 minutos**, en la cabecera `Authorization: Bearer`. Lleva el identificador
  público de la cuenta, sus roles y el nombre de usuario; nunca el correo ni datos sensibles. La tienda lo guarda **solo en
  memoria**, nunca en `localStorage`. Se firma con una clave secreta del servidor (`APP_JWT_SECRET`).
- **Token de renovación: aleatorio opaco de 256 bits, 30 días**, en una cookie `HttpOnly; Secure; SameSite=Strict` limitada a
  `/api/v1/auth`. En la base de datos solo se guarda su huella (SHA-256).
- **Rotación con detección de robo:** cada token de renovación vale una sola vez y se cambia por otro de la misma
  «familia». Si se presenta uno ya usado, se anula toda la familia.
- **Sin estado en el servidor para el acceso**; con estado solo para la renovación (tabla `refresh_tokens`).
- **Inicio de sesión solo con el correo** (decisión de la empresa).
- **Bloqueo tras 5 intentos fallidos por correo durante 15 minutos**, aplicado igual a cualquier correo, y respuesta y tiempo
  idénticos si el correo no existe.
- **CSRF desactivado** en la API porque el acceso va en una cabecera; las dos operaciones con cookie (renovar y cerrar sesión)
  exigen la cabecera `X-LV-Client: web` además de `SameSite=Strict`.
- **HS256 en lugar de una firma asimétrica:** al ser un único servidor que emite y valida, una clave compartida es
  suficiente. Si algún día otro servicio tuviera que validar tokens sin poder emitirlos, se cambiaría a firma asimétrica.

## Consecuencias

- Un token de acceso robado vale como máximo 15 minutos; cerrar sesión no lo anula antes (por eso es corto).
- El frontend debe renovar la sesión al caducar el acceso y al arrancar (la cookie permite recuperar la sesión al recargar).
- El contador de intentos fallidos está en memoria hasta que haya varias instancias (historia E14-01).
- Detalle del funcionamiento y de las defensas en `docs/identity.md`.
