# El contrato de la API (OpenAPI)

El contrato es un documento que describe con todo detalle cómo se habla con el servidor: qué direcciones hay, qué
datos hay que enviar a cada una, qué responde y qué errores puede dar. Es lo que mantiene a la tienda (Angular) y al
servidor (Spring) siempre de acuerdo.

## Cómo funciona

```
Código del servidor (controladores y sus anotaciones)
        │  springdoc lo lee y lo describe
        ▼
contracts/openapi.json          ← el contrato, guardado en Git
        │  ng-openapi-gen lo convierte en código de Angular
        ▼
frontend/projects/api/src/lib/generated/   ← el cliente: tipos y funciones para llamar al servidor
```

Nadie escribe a mano el documento ni el cliente. Se generan, se suben a Git y se vigilan.

## Cuando se cambia la API

Si se añade o cambia un endpoint, un campo o un error, hay que actualizar el contrato y el cliente. Desde la raíz del
proyecto, con Docker abierto:

```powershell
.\tools\update-api-contract.ps1
```

Eso arranca el servidor en una prueba, guarda el contrato en `contracts\openapi.json` y regenera el cliente de Angular.
Después se suben a Git **los dos cambios juntos** (`contracts\openapi.json` y `frontend\projects\api`).

## Qué vigila la integración continua

| Comprobación | Qué detecta |
| --- | --- |
| Prueba `OpenApiContractTests` (backend) | Que el contrato que genera el servidor sea idéntico al guardado en `contracts/openapi.json`. Si alguien cambia la API y se olvida de actualizar el contrato, falla |
| Paso «cliente de la API al día» (frontend) | Que, al regenerar el cliente a partir del contrato guardado, no cambie nada. Si alguien edita el cliente a mano o se olvida de regenerarlo, falla |
| Compilación del frontend | Que lo que usan las pantallas siga existiendo: si el contrato quita un campo que una pantalla utiliza, no compila |

## Reglas

- **No se edita `contracts/openapi.json` ni `frontend/projects/api/src/lib/generated`.** Se regeneran.
- **El contrato describe lo que el servidor hace.** Las anotaciones de los controladores (`@Operation`, `@ApiResponse`,
  `@Schema`) son documentación que se mantiene junto al código. Cada operación tiene un `operationId` estable
  (`registerAccount`, `verifyEmail`, `resendVerification`): de él sale el nombre de la función del cliente, así que no
  se cambia sin necesidad.
- **Los errores tienen forma fija** (`Problem`, con su `code`), descritos en el contrato: el frontend decide qué mostrar
  por el `code`, nunca por el texto.
- **No hay Swagger UI** (la página web de pruebas de springdoc): no hace falta y ampliaría la superficie de ataque. En
  producción ni siquiera se publica el documento (`springdoc.api-docs.enabled: false`).

## Desarrollo local

El servidor de desarrollo de Angular reenvía todo lo que empieza por `/api` al servidor de Spring (`localhost:8080`),
con `proxy.conf.json`. Así las pantallas llaman a una dirección relativa, sin CORS. En producción, un proxy inverso hará
lo mismo: la tienda y la API vivirán bajo el mismo dominio.

## Dirección base del cliente

El contrato declara el servidor como `/` (relativo, para que el documento no cambie en cada ejecución) y el generador
toma ese valor como dirección base por defecto. Como las rutas ya empiezan por `/`, las llamadas saldrían como
`//api/v1/...`, que el navegador interpreta como otra web. Por eso `app.config.ts` de cada aplicación fija
`provideApiConfiguration('')`: las llamadas van a la misma dirección que la página. Una prueba (`app.config.spec.ts`)
vigila que no se pierda. Lo configura `tools/setup-api-client.mjs`.

## Cómo se usa el cliente desde Angular

Las llamadas pasan por el servicio `Api` que genera ng-openapi-gen; cada operación del contrato es una función con sus
tipos (el nombre y la ruta exactos se ven en `frontend/projects/api/src/lib/generated`):

```ts
const api = inject(Api);
await firstValueFrom(api.invoke(registerAccount, { body: { username, email, password, acceptTerms: true } }));
```

Los errores llegan como `HttpErrorResponse` cuyo `error` es un `Problem`.
