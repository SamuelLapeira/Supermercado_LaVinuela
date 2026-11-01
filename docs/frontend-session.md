# La sesión en el navegador (librería `core`)

Qué hace la tienda para que una persona esté «dentro» sin poner en riesgo su cuenta. Es la parte del navegador de lo que
describe `docs/identity.md` (servidor) y la ADR-016.

## Qué se guarda y dónde

| Dato | Dónde | Por qué |
| --- | --- | --- |
| **Token de acceso** (15 minutos) | Solo en la **memoria de la página** | No se puede robar leyendo el almacenamiento del navegador; al cerrar la pestaña desaparece |
| **Token de renovación** (30 días) | Una **cookie `HttpOnly`** que gestiona el navegador | JavaScript no puede leerla, así que un fallo de seguridad de la página no la roba |
| **Indicador «había sesión»** | `localStorage`, un «sí» o un «no» | Para no pedir al servidor que renueve la sesión de quien nunca ha entrado. **No contiene ningún token ni dato personal** |

## Cómo se mantiene la sesión

1. **Al arrancar** la aplicación, si el indicador dice que había sesión, se pide al servidor que la renueve con la cookie. Si
   lo consigue, la persona aparece «dentro» sin escribir nada. Si no había indicador, ni se pregunta.
2. **En cada llamada a `/api/`** un interceptor añade la cabecera `X-LV-Client: web` y, si hay sesión, el token de acceso.
3. **Cuando el token caduca** (el servidor responde 401), el interceptor renueva la sesión **una sola vez** y repite la
   llamada con el token nuevo. Si varias llamadas fallan a la vez, esperan **la misma** renovación: el servidor cierra la
   sesión entera si recibe dos renovaciones con el mismo token.
4. **Si no se puede renovar**, la persona queda fuera y el error llega a la pantalla.
5. **Al salir**, se avisa al servidor (que anula la sesión) y se olvida todo aquí, aunque el servidor no responda.

## Piezas

| Pieza | Para qué |
| --- | --- |
| `SessionService` | Estado de la sesión (señales `status`, `user`, `accessToken`) y las operaciones entrar, renovar, salir y recuperar |
| `sessionInterceptor` | Añade cabecera y token y renueva al caducar |
| `authGuard` | Protege rutas: sin sesión, lleva a `/entrar?volver=...` |
| `provideSession()` | Recupera la sesión al arrancar |

Se configuran en `app.config.ts` de la tienda. El panel de gestión usará lo mismo cuando tenga pantallas protegidas.

## Una limitación conocida

Mientras no haya sesión guardada, el navegador no hace ninguna petición extra. Pero si el indicador dice que había sesión y la
cookie ya ha caducado, se verá un error 401 en la consola del navegador al arrancar (es esperado: el servidor responde que esa
sesión ya no vale, y el indicador se borra).
