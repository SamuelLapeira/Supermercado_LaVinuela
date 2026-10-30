# Puesta en marcha en Windows (PowerShell)

Los scripts con extensión `.sh` son para Linux y Mac. En Windows se usan los equivalentes `.ps1`.

## 1. Qué hay que instalar

| Herramienta | Versión | Cómo comprobarla (en PowerShell) |
| --- | --- | --- |
| JDK (por ejemplo Eclipse Temurin) | **25** | `java -version` debe decir 25 |
| Maven | 3.9.6 o superior | `mvn -v` |
| Node.js | **24.15 o superior** (Angular 22 lo exige; también vale 22.22.3 o 26) | `node -v` |
| Docker Desktop | Abierto y en marcha | `docker version` |
| Git (opcional, recomendado) | Cualquiera reciente | `git --version` |

Después de instalar algo, **cierra y vuelve a abrir PowerShell** para que lo reconozca.

## 2. Descomprimir el proyecto

Descomprime `lavinuela-shop-sprint1.zip` en una ruta corta, sin espacios ni acentos, por ejemplo:

```
C:\proyectos\lavinuela-shop
```

Este ZIP **sustituye** al anterior (`lavinuela-shop-E0-01.zip`): contiene todo lo anterior más lo nuevo. Si
ya descomprimiste el antiguo, bórralo o descomprime el nuevo en otra carpeta; no los mezcles.

## 3. Cómo queda la estructura

```
lavinuela-shop\
  backend\        API (Java 25 + Spring Boot).  Ya viene en el ZIP.
  frontend\       Angular 22 (tienda y panel).  Se crea con el script del paso 6.
  docs\           Documentación y decisiones (adr\)
  tools\          Scripts (.ps1 para Windows, .sh para Linux/Mac)
  docker\         Configuración de MySQL para el entorno local
  docker-compose.yml   Entorno local (MySQL, Valkey y Mailpit)
```

`backend` y `frontend` son **carpetas independientes**: cada una se compila, se prueba y se despliega por
separado, y solo se comunican por HTTP. Más adelante se podrían separar en dos repositorios sin cambiar nada.

## 4. Abrir PowerShell en la carpeta del proyecto

```powershell
cd C:\proyectos\lavinuela-shop
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
```

La segunda línea permite ejecutar los scripts **solo en esa ventana**; no cambia nada permanente del sistema.

## 5. Backend

```powershell
.\tools\dev-up.ps1            # levanta MySQL, Valkey y Mailpit (la primera vez crea el fichero .env)
.\tools\load-env.ps1          # carga las contraseñas de .env en esta ventana
cd backend
mvn verify                    # compila, ejecuta todas las pruebas y comprueba la arquitectura
mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

Notas:

- Docker Desktop tiene que estar **abierto** antes de `dev-up.ps1`.
- Las comillas en `"-Dspring-boot.run.profiles=local"` son necesarias en PowerShell.
- `mvn verify` ejecuta también pruebas contra un MySQL real: necesitan Docker. Si Docker no está en
  marcha, esas pruebas se saltan (no fallan).
- La API queda en `http://localhost:8080`. De momento no tiene pantallas ni endpoints: es el esqueleto.
- Los correos de prueba se ven en `http://127.0.0.1:8025` (Mailpit).

Para parar el entorno:

```powershell
.\tools\dev-down.ps1          # conserva los datos
.\tools\dev-down.ps1 -Reset   # borra también los datos locales
```

## 6. Frontend (Angular)

Si `node -v` da una versión anterior a 24.15, el script se detiene con un aviso de versión: actualiza Node desde https://nodejs.org (versión LTS) y vuelve a abrir PowerShell.

Necesita Node.js e internet. Desde la carpeta raíz del proyecto:

```powershell
.\tools\bootstrap-frontend.ps1
cd frontend
npx.cmd ng build tienda
npx.cmd ng build panel
```

Para ver una aplicación en el navegador:

```powershell
cd frontend
npx.cmd ng serve tienda --host 127.0.0.1
```

y abrir `http://127.0.0.1:4200` (se para con Ctrl+C). El `--host 127.0.0.1` evita un fallo habitual de Windows en
el que `localhost` no responde. `tienda` y `panel` no pueden ir a la vez en el mismo puerto.

**Cuidado con la carpeta:** los scripts de `tools` se ejecutan desde la raíz del proyecto y los comandos `ng` desde
`frontend`. Escribir `Get-Location` al final de cada bloque te dice dónde estás antes de pegar el siguiente.

Esto crea la carpeta `frontend` con las aplicaciones `tienda` y `panel` y las librerías `ui`, `core`, `api` y
`util`. Si algún parámetro del generador no existiera en la versión de Angular instalada, el script se
detiene con un mensaje; pégamelo y lo ajusto.

## 7. Problemas habituales

| Síntoma | Causa y solución |
| --- | --- |
| `mvn` o `java` «no se reconoce» | No está instalado o falta reabrir PowerShell |
| `java -version` dice una versión anterior a 25 | Hay otro JDK primero en el `PATH`; ajusta `JAVA_HOME` o desinstala el otro |
| `dev-up.ps1` falla al arrancar contenedores | Docker Desktop no está abierto, o el puerto 3306 está ocupado (cámbialo con `DB_PORT` en `.env`) |
| La aplicación no conecta a la base de datos | Falta ejecutar `.\tools\load-env.ps1` en esa misma ventana |
| Error raro en el contenedor de MySQL al crear usuarios | El fichero `docker\mysql\init\01-create-users.sh` debe conservar saltos de línea tipo Linux (LF). No lo edites con el Bloc de notas |
| «La ejecución de scripts está deshabilitada» | Ejecuta el `Set-ExecutionPolicy` del paso 4 |
| Cualquier otro error | Copia el mensaje completo y pégamelo |

## 8. Git (opcional)

```powershell
git init
git add .
git commit -m "Sprint 1: estructura, entorno local, base de datos y núcleo compartido"
```

El fichero `.gitattributes` del proyecto mantiene los saltos de línea correctos.
