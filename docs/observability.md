# Observabilidad (historia E0-05)

Qué hay para saber qué está pasando en la aplicación cuando está funcionando, y para encontrar la causa de
un problema sin adivinar.

## Identificador de seguimiento de cada petición

Cada petición recibe un identificador (`X-Request-Id`):

- Si el cliente envía uno válido (8 a 64 caracteres: letras, números, punto, guion o guion bajo), se respeta.
  Cualquier otro valor se descarta, para que nadie pueda inyectar texto falso en los registros.
- Si no, se genera uno nuevo.
- Se devuelve **siempre** en la cabecera `X-Request-Id` de la respuesta y se añade a **todos** los registros de
  esa petición (campo `requestId`).
- Las respuestas de error lo incluyen como `traceId`:

```json
{
  "type": "urn:lavinuela:problem:resource-not-found",
  "status": 404,
  "detail": "No hemos encontrado lo que buscas.",
  "code": "RESOURCE_NOT_FOUND",
  "traceId": "01KB3Z6Q8M2R7V1N4P9XJ5C0TD"
}
```

**Cómo se usa:** si una persona reporta un problema, basta con pedirle ese código. Buscándolo en los registros
aparece todo lo que ocurrió en esa petición, de principio a fin.

## Registros

| Perfil | Formato |
| --- | --- |
| Local | Texto legible: `INFO [01KB3Z6Q...] GET /api/v1/... -> 200 (12 ms)` |
| Producción | Una línea JSON por registro en formato **ECS**, lista para cualquier sistema de registros |

Cada petición deja una línea con el método, la ruta, el estado y la duración (las de error de servidor, como
aviso). Las sondas de salud y las métricas no llenan el registro (solo aparecen en nivel de depuración).

### Qué nunca se escribe en un registro

- Contraseñas, tokens de acceso o de refresco, cabecera `Authorization`.
- Firmas y claves de Redsys, y por supuesto datos de tarjeta.
- Cuerpos de petición o de respuesta.
- La cadena de consulta de la URL (puede llevar datos personales).
- Correos y teléfonos completos: solo enmascarados con `Masking.email(...)` y `Masking.phone(...)`. Para
  identificar a alguien se usa su identificador público.

## Salud

| Punto | Para qué | Respuesta |
| --- | --- | --- |
| `/actuator/health` | Estado general | `{"status":"UP"}` |
| `/actuator/health/liveness` | «¿El proceso está vivo?». Si falla, se reinicia | 200 o 503 |
| `/actuator/health/readiness` | «¿Puede recibir tráfico?». Incluye la conexión con la base de datos; si falla, se deja de enviarle peticiones, pero no se reinicia | 200 o 503 |

Las respuestas **no revelan detalles** (nombres de servicios, de bases de datos, versiones): solo `UP` o `DOWN`.

## Métricas

`/actuator/prometheus` publica las métricas en el formato estándar de Prometheus (lo entienden también los
servicios gestionados de las nubes): memoria y procesos de Java, peticiones HTTP con su latencia (con
percentiles), conexiones a la base de datos, etc. Todas llevan la etiqueta `application="lavinuela-shop"`.

`/actuator/info` muestra la versión y la fecha de compilación.

## Dónde escuchan

| Perfil | Dónde | Notas |
| --- | --- | --- |
| Local y producción | Puerto **8081**, solo desde la propia máquina (`127.0.0.1`) | Se cambia con `MANAGEMENT_PORT` y `MANAGEMENT_ADDRESS` |
| Pruebas | El mismo puerto que la aplicación | Para poder probarlos sin puertos fijos |

**Importante en producción:** el puerto de gestión **no se publica a internet**. Solo debe alcanzarlo la red
interna (el sistema que vigila la salud y recoge las métricas). La dirección se fija según el alojamiento.

## Probarlo en local

Con el entorno levantado (`.\tools\dev-up.ps1` y `.\tools\load-env.ps1`) y la aplicación arrancada:

```powershell
Invoke-RestMethod http://127.0.0.1:8081/actuator/health
Invoke-RestMethod http://127.0.0.1:8081/actuator/health/readiness
Invoke-WebRequest http://127.0.0.1:8081/actuator/prometheus | Select-Object -ExpandProperty Content | Select-Object -First 20
Invoke-WebRequest http://localhost:8080/algo-que-no-existe -SkipHttpErrorCheck
```

La última petición devuelve un 404 en formato de error estándar, con su `traceId`.

## Lo que queda para más adelante

- **Trazas distribuidas** con OpenTelemetry: el identificador de traza ocupará el lugar del identificador de
  petición sin cambiar nada más. Se incorpora cuando se decida dónde se despliega y a qué sistema se envían.
- **Métricas de negocio** (pedidos, pagos fallidos, reservas caducadas, aciertos del servicio de rutas): se
  añaden junto con cada funcionalidad.
- **Alertas** (tasa de errores, latencia, pedidos pendientes de pago, etc., Fase 3 apartado 14): dependen del
  sistema de métricas que se elija con el alojamiento.
- **Identificador de usuario en los registros**: llega con el inicio de sesión (historia E1-02).
