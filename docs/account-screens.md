# Pantallas de registro y verificación (tienda)

Dos pantallas de la tienda (Angular) que usan el servidor de cuentas (`docs/identity.md`) a través del cliente generado
del contrato (`docs/api-contract.md`).

## El recorrido

```
/registro  ──►  la persona escribe usuario, correo y contraseña, y acepta las condiciones
   │            (el servidor crea la cuenta pendiente y envía un código al correo)
   ▼
/verificar ──►  escribe el código de 6 cifras  ──►  «¡Cuenta activada!»
                ¿no llega? «Reenviar código» (con cuenta atrás de 60 s)
```

## Pantalla de registro (`/registro`)

- **Ayuda mientras se escribe:** las cuatro condiciones de la contraseña (10 caracteres, mayúscula, minúscula y número) se
  van marcando según se cumplen. El botón «Mostrar contraseña» permite ver lo escrito.
- **Validación en dos niveles:** el navegador avisa con las mismas reglas del servidor, pero la que cuenta es la del
  servidor (que además rechaza contraseñas muy comunes y las que contienen el usuario o el correo). Los errores del
  servidor aparecen junto al campo que corresponde y desaparecen cuando la persona lo corrige.
- **Errores que se entienden:** usuario cogido, datos incorrectos, o «no hemos podido conectar con el servidor» si no hay
  conexión. La pantalla decide por el **código** del error, nunca por su texto.
- **Al terminar:** lleva a `/verificar`. El correo viaja en el estado del historial del navegador, no en la dirección web,
  así que no queda escrito en la URL.

## Pantalla de verificación (`/verificar`)

- Si se llega desde el registro, el correo ya se conoce y se enseña enmascarado (`a***@example.com`); si se entra
  directamente, se pide también el correo.
- El campo del código acepta solo cifras (si se pega «482 915» queda 482915), muestra el teclado numérico en el móvil y
  permite que el sistema proponga el código recibido por SMS o correo (`autocomplete="one-time-code"`).
- **Código incorrecto o caducado:** se avisa junto al campo. **Cinco intentos fallidos:** el campo se bloquea hasta que se
  pida un código nuevo.
- **Reenviar el código:** el botón cuenta atrás desde 60 segundos (el servidor exige esa espera). El mensaje es neutro
  («si hay una cuenta pendiente con ese correo…»), igual que la respuesta del servidor, para no revelar qué correos
  tienen cuenta.
- Al acertar, «¡Cuenta activada!». Hasta que exista el inicio de sesión, el mensaje lo explica con honestidad.

## Accesibilidad

- Todos los campos tienen etiqueta visible, ayuda y error enlazados (`aria-describedby`, `aria-invalid`).
- Al enviar con errores, el foco pasa al primer campo con problema.
- Los avisos de error se anuncian al aparecer; las condiciones de la contraseña tienen texto además de la marca
  (cumplida o pendiente), no solo color o símbolo.
- Objetivos táctiles de 44 px, foco siempre visible, modo claro y oscuro.

## Cómo probarlas en local

Con el entorno levantado y el servidor arrancado (`docs/identity.md`, «Probarlo en local»), en otra ventana:

```powershell
cd frontend
npx.cmd ng serve tienda --host 127.0.0.1
```

Abrir `http://127.0.0.1:4200` (lleva a `/registro`). El servidor de desarrollo reenvía `/api` al servidor de Spring. El
correo con el código se ve en Mailpit: `http://127.0.0.1:8025`.

## Dónde está el código

`frontend/projects/tienda/src/app/features/account/`: las dos pantallas (`register.page`, `verify-email.page`), el marco
común (`auth-layout`), las reglas del formulario (`account-rules`) y **un único servicio** (`account.service`) que conoce
el cliente generado.
