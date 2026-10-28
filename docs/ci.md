# Integración continua (historia E0-07)

Cada vez que se sube un cambio a `main`, o se abre un *pull request*, GitHub ejecuta automáticamente dos
tareas en paralelo (fichero `.github/workflows/ci.yml`). Si cualquiera falla, el cambio aparece marcado con
una ✗ roja y no debería aceptarse.

## Qué comprueba

| Tarea | Qué hace |
| --- | --- |
| **Backend** | Compila con Java 25 con los avisos del compilador como error, ejecuta todas las pruebas (incluidas las de MySQL real con Testcontainers), las reglas de arquitectura y la verificación de módulos. Falla si alguna prueba se salta |
| **Frontend** | Instala dependencias de forma reproducible (`npm ci`), pasa ESLint (estilo, accesibilidad y fronteras entre librerías), compila `tienda` y `panel` en modo de producción (TypeScript estricto y límites de tamaño) y ejecuta las pruebas |

## Cómo verlo

En GitHub, pestaña **Actions** del repositorio: https://github.com/SamuelLapeira/Supermercado_LaVinuela/actions

Cada ejecución muestra los pasos; al abrir uno fallido se ve el mensaje de error completo. Si falla una
prueba del backend, se guardan los informes como descarga durante 7 días.

## Lo mismo en local (antes de subir)

```powershell
# Backend (con Docker Desktop abierto)
cd backend
mvn verify

# Frontend
cd ..\frontend
npm ci
npx.cmd ng lint
npx.cmd ng build tienda
npx.cmd ng build panel
npx.cmd ng test --watch=false
```

## Recomendado: proteger la rama `main`

En GitHub: **Settings → Branches → Add branch protection rule** para `main`, con:

- *Require a pull request before merging*.
- *Require status checks to pass before merging*, y marcar **Backend** y **Frontend**.

Así nadie (tampoco nosotros) puede aceptar un cambio que rompa la compilación o las pruebas.

## Actualizaciones automáticas

`.github/dependabot.yml` pide a GitHub que cada lunes abra *pull requests* con las actualizaciones de
dependencias (Maven, npm, acciones de GitHub y Docker). La integración continua las comprueba antes de
aceptarlas. Los saltos de versión mayor de Spring Boot o Angular se hacen con calma, siguiendo el calendario
de actualizaciones del proyecto.

## Qué queda para más adelante

- **Contrato OpenAPI y cliente de Angular generado.** Se añade con el primer endpoint real (historia E1-01):
  hasta entonces no hay nada que describir, y montar la maquinaria vacía solo añadiría ruido.
- Análisis de dependencias vulnerables y de la imagen de contenedores (se incorporan antes de la revisión de
  seguridad, historia E14-04).
