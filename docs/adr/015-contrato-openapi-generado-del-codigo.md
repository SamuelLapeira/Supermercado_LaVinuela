# ADR-015: Contrato OpenAPI generado del código y cliente de Angular generado del contrato

**Estado:** Aceptada

## Contexto

El frontend (Angular) y el backend (Spring) son proyectos separados que solo se comunican por HTTP. Si cada uno
escribe a mano su parte, se desincronizan sin que nadie lo note hasta que falla una pantalla.

## Decisión

- **El contrato sale del código** (enfoque «código primero»): springdoc-openapi 3.1 (la serie construida sobre Spring
  Boot 4.1) lee los controladores y sus anotaciones y genera el documento OpenAPI. Solo el documento JSON; sin Swagger UI.
- **El contrato se guarda en Git** (`contracts/openapi.json`) con un orden de claves estable, de modo que solo cambia
  cuando cambia la API de verdad y los cambios se ven en las revisiones.
- **El cliente de Angular se genera del contrato** con ng-openapi-gen y se guarda también en Git
  (`frontend/projects/api/src/lib/generated`), de modo que las compilaciones no dependen de ningún generador.
- **Dos vigilantes en la integración continua:** una prueba del backend que compara el contrato generado con el guardado,
  y un paso del frontend que comprueba que regenerar el cliente no cambia nada.
- **Los errores forman parte del contrato:** `Problem` (RFC 9457) con su `code` estable.
- **En producción el documento no se publica.** El servidor de desarrollo de Angular reenvía `/api` al backend; en
  producción, un proxy inverso servirá ambos bajo el mismo dominio (sin CORS).

## Consecuencias

- Cambiar la API exige regenerar el contrato y el cliente (`tools/update-api-contract.ps1`) y subirlos juntos.
- Los controladores llevan anotaciones de OpenAPI (`@Operation`, `@ApiResponse`, `@Schema`) que hay que mantener al día.
- Las pruebas del contrato necesitan Docker, como el resto de pruebas con la aplicación entera.
- Detalle del flujo en `docs/api-contract.md`.
