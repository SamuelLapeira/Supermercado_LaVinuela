# Cuentas y registro (historia E1-01)

## Sesiones: entrar con el correo

Se entra **con el correo electrónico y la contraseña** (el nombre de usuario es el nombre con el que te saludamos, no sirve para entrar).

## Qué se pide al registrarse

Solo lo imprescindible, para que registrarse sea rápido:

| Se pide al registrarse | Se pide más adelante |
| --- | --- |
| Nombre de usuario, correo electrónico y contraseña. Aceptar las condiciones y la política de privacidad | Nombre y apellidos reales, teléfono y direcciones de entrega: en el **primer pedido** |

El correo se confirma con un **código de 6 cifras** que llega a la bandeja de entrada. Hasta que se confirme, la cuenta
existe pero no se puede usar.

## El recorrido

```
Registro (usuario, correo, contraseña)
   └─ la cuenta queda PENDIENTE y se envía un código al correo
        └─ la persona escribe el código ──► cuenta ACTIVA
        └─ ¿no llega o ha caducado? ──► «Reenviar código» (con espera de 60 s)
```

## Reglas

| Dato | Regla |
| --- | --- |
| Nombre de usuario | De 3 a 30 caracteres: letras, números, punto, guion y guion bajo; empieza y termina por letra o número. No distingue mayúsculas (`Ana` y `ana` son el mismo). Hay nombres reservados (`admin`, `soporte`, `lavinuela`...) |
| Correo | Formato válido, hasta 254 caracteres. No distingue mayúsculas |
| Contraseña | De **10 a 128 caracteres**, con **al menos una mayúscula, una minúscula y un número**. Los símbolos, espacios, acentos y emojis se admiten pero no se obligan. Además se rechazan las contraseñas más comunes («Password123»), las repeticiones y las que contienen el nombre de usuario o la parte del correo antes de la arroba. El mensaje de error dice exactamente qué le falta |
| Código de verificación | 6 cifras, **15 minutos** de validez, **5 intentos** fallidos y hay que pedir otro. Espera de **60 segundos** entre códigos y máximo **5 por hora** por cuenta. Un código nuevo anula los anteriores |

## Interfaz (API)

Los tres puntos son públicos, no necesitan haber iniciado sesión. Los errores usan el formato estándar
`application/problem+json` con un `code` estable y, si procede, `errors` con el campo que falló.

### `POST /api/v1/auth/register` → 202

```json
{ "username": "ana.lopez", "email": "ana@example.com", "password": "Una-contraseña-larga-2026", "acceptTerms": true }
```

Respuesta (igual si el correo era nuevo o ya existía, para no revelar quién tiene cuenta):

```json
{ "email": "a***@example.com", "codeValiditySeconds": 900, "resendAfterSeconds": 60 }
```

| Código HTTP | `code` | Cuándo |
| --- | --- | --- |
| 400 | `VALIDATION_FAILED` | Datos incorrectos: `errors` indica el campo (`username`, `email`, `password`, `acceptTerms`) |
| 409 | `USERNAME_TAKEN` | Ese nombre de usuario ya está en uso |

### `POST /api/v1/auth/verify-email` → 204

```json
{ "email": "ana@example.com", "code": "123456" }
```

| Código HTTP | `code` | Cuándo |
| --- | --- | --- |
| 400 | `VALIDATION_FAILED` | El código no tiene 6 cifras |
| 422 | `VERIFICATION_CODE_INVALID` | Código incorrecto, o el correo no tiene una cuenta pendiente (misma respuesta en ambos casos) |
| 422 | `VERIFICATION_CODE_EXPIRED` | El código ha caducado |
| 429 | `VERIFICATION_TOO_MANY_ATTEMPTS` | Cinco intentos fallidos: hay que pedir un código nuevo |

### `POST /api/v1/auth/resend-verification` → 202

```json
{ "email": "ana@example.com" }
```

Responde siempre `{ "resendAfterSeconds": 60 }`, exista o no la cuenta. Si no se ha superado ningún límite, se envía un
código nuevo.

## Seguridad

- **Contraseñas:** huella con **Argon2id** (la recomendada por OWASP; 19 MiB de memoria, 2 iteraciones, 1 hilo), con
  sal aleatoria propia de cada contraseña y en formato estándar. La contraseña nunca se guarda ni se escribe en un
  registro. Los parámetros se recalibrarán con el servidor real.
- **Códigos:** en la base de datos solo se guarda una huella (HMAC-SHA256 con una clave secreta del servidor, la
  «pimienta», y el identificador de la cuenta). Un código de 6 cifras tiene un millón de posibilidades, así que sin la
  clave secreta, que no está en la base de datos, quien robara los datos no podría recuperarlos. El código en claro solo
  existe en memoria mientras se envía el correo.
- **Intentos fallidos:** se guardan aunque la petición termine en error; si no, se podrían probar códigos sin límite.
- **No se revela qué correos tienen cuenta:** registrarse con un correo que ya existe da la misma respuesta. Si la cuenta
  estaba pendiente, recibe un código nuevo; si ya estaba activa, su titular recibe un aviso por correo. Lo único que se
  puede saber es si un nombre de usuario está cogido, como en cualquier sitio con nombres de usuario.
- **Correos:** se envían después de confirmar el registro. Si el servidor de correo falla, la cuenta ya está creada y la
  persona puede pedir otro código.
- **Consentimientos:** cada aceptación de las condiciones y de la política de privacidad queda registrada con la
  versión del texto aceptado.
- **Registros (logs):** nunca llevan contraseñas, códigos ni el correo completo (solo enmascarado: `a***@example.com`).

## Probarlo en local

1. Levanta el entorno (crea la clave secreta `APP_CODE_PEPPER` en `.env` si todavía no existe) y carga las variables:

   ```powershell
   .\tools\dev-up.ps1
   .\tools\load-env.ps1
   ```

2. Arranca la aplicación:

   ```powershell
   cd backend
   mvn spring-boot:run "-Dspring-boot.run.profiles=local"
   ```

3. Regístrate (desde otra ventana de PowerShell). El cuerpo se envía como bytes UTF-8 para que las eñes lleguen bien:

   ```powershell
   $body = @{ username = "samuel"; email = "samuel@example.com"; password = "Una-contraseña-larga-2026"; acceptTerms = $true } | ConvertTo-Json
   Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/auth/register -ContentType "application/json; charset=utf-8" -Body ([System.Text.Encoding]::UTF8.GetBytes($body))
   ```

4. Abre **Mailpit** en el navegador (`http://127.0.0.1:8025`): ahí está el correo con el código.

5. Confirma el correo con ese código:

   ```powershell
   $v = @{ email = "samuel@example.com"; code = "123456" } | ConvertTo-Json
   Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/auth/verify-email -ContentType "application/json" -Body $v
   ```

Las pantallas de la tienda están descritas en `docs/account-screens.md` y el contrato de la API en `docs/api-contract.md`.

## Iniciar y cerrar sesión (historia E1-02)

### Cómo funciona

```
POST /api/v1/auth/login  (correo + contraseña)
   ├─ token de acceso (JWT, 15 minutos)  ──► en la respuesta; la tienda lo guarda solo en memoria
   └─ token de renovación (30 días)      ──► en una cookie que JavaScript no puede leer

cada petición protegida:   Authorization: Bearer <token de acceso>
al caducar el acceso:      POST /api/v1/auth/refresh  → tokens nuevos (el de renovación se cambia por otro)
al salir:                  POST /api/v1/auth/logout   → se anula la sesión y se borra la cookie
```

### Defensas

| Qué | Cómo |
| --- | --- |
| Token de acceso robado | Dura solo **15 minutos** y no se guarda en el navegador (vive en la memoria de la página), así que no se puede robar de `localStorage` |
| Token de renovación robado | Va en una cookie **HttpOnly** (JavaScript no la lee), **Secure** (solo HTTPS), **SameSite=Strict** (no se envía desde otras webs) y limitada a `/api/v1/auth`. En la base de datos solo se guarda su huella |
| Copia de un token de renovación | **Cada token vale una sola vez**: al renovar se cambia por otro. Si alguien presenta uno ya usado, se anula la sesión entera y las dos partes tienen que volver a entrar |
| Adivinar contraseñas | **5 fallos con un mismo correo bloquean 15 minutos**, y el bloqueo se aplica **igual a cualquier correo, exista o no**, para no delatar qué cuentas existen |
| Averiguar qué correos existen | Correo desconocido y contraseña incorrecta dan **la misma respuesta**, y cuando no hay cuenta se comprueba una contraseña ficticia para que **tarde lo mismo** |
| Cuenta sin verificar | Solo se avisa («confirma tu correo») cuando la contraseña es correcta |
| Otra web que intenta renovar o cerrar tu sesión | Esas dos operaciones exigen una cabecera propia (`X-LV-Client: web`) que una web ajena no puede añadir, además de `SameSite=Strict` |
| Errores de seguridad | Sin sesión (401) y sin permiso (403) responden con el mismo formato estándar que el resto de la API |

### Puntos de la API

| Punto | Para qué | Errores |
| --- | --- | --- |
| `POST /api/v1/auth/login` | Entrar con correo y contraseña | 401 `AUTH_INVALID_CREDENTIALS`, 403 `AUTH_EMAIL_NOT_VERIFIED`, 423 `AUTH_ACCOUNT_LOCKED` |
| `POST /api/v1/auth/refresh` | Renovar la sesión (con la cookie) | 401 `AUTH_TOKEN_EXPIRED`, 401 `AUTH_TOKEN_REUSED`, 403 `ACCESS_DENIED` |
| `POST /api/v1/auth/logout` | Cerrar la sesión (con la cookie) | 403 `ACCESS_DENIED` |
| `GET /api/v1/me` | Quién soy (**protegido**: exige el token de acceso) | 401 `AUTH_REQUIRED`, 401 `AUTH_TOKEN_EXPIRED` |

**Desde ahora toda la API exige sesión**, salvo `/api/v1/auth/**`, el contrato OpenAPI y los puntos de control del sistema.

### Límites que conviene conocer

- El contador de intentos fallidos vive **en memoria**: se reinicia al reiniciar la aplicación y no se comparte entre varias
  instancias. Cuando haya varias pasará a Redis (historia E14-01).
- Dos pestañas que renuevan a la vez con el mismo token provocarían un cierre de sesión por «reutilización». La tienda
  evita esa carrera renovando una sola vez a la vez (se hará con la pantalla de inicio de sesión).
- Un token de acceso ya emitido sigue valiendo hasta que caduca (15 minutos) aunque se cierre la sesión: por eso es tan corto.

### Probarlo en local

Con el servidor arrancado, tras registrarte y confirmar el correo:

```powershell
$login = @{ email = "samuel@example.com"; password = "Una-contraseña-larga-2026" } | ConvertTo-Json
$r = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/auth/login -ContentType "application/json; charset=utf-8" -Body ([System.Text.Encoding]::UTF8.GetBytes($login))
$r.user
Invoke-RestMethod -Uri http://localhost:8080/api/v1/me -Headers @{ Authorization = "Bearer $($r.accessToken)" }
```

## Qué queda para las siguientes historias

- **Pantalla de inicio de sesión y cabecera de la tienda con la persona identificada** (E1-02, parte de la tienda).
- **Recuperar la contraseña** (E1-03) y **perfil, direcciones y teléfono** (en el primer pedido, E4 y E1-04).
- **Límite de peticiones por dirección IP** (E14-01): hoy los límites son por cuenta.
- **Textos legales definitivos** (E14-03): la versión de los textos aceptados es provisional (`2026-10`).
