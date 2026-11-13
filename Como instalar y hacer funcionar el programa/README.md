# Cómo instalar y hacer funcionar ParkFlow

Esta guía está pensada para instalar ParkFlow en un computador Windows y usarlo localmente para revisión o demostración. El camino recomendado usa Docker Desktop: no hace falta instalar Java, Maven, Node.js ni PostgreSQL por separado.

## 1. Requisitos

- Windows 10 u 11 de 64 bits.
- Git para Windows.
- Docker Desktop instalado y abierto, usando contenedores Linux con WSL 2.
- Acceso a internet durante la primera compilación para descargar imágenes y dependencias.
- Espacio libre suficiente para imágenes y datos de Docker.

Comprueba que Docker responde desde PowerShell:

```powershell
docker info
docker compose version
git --version
```

Si `docker info` no muestra una sección `Server`, abre Docker Desktop y espera a que indique que está iniciado.

## 2. Descargar el proyecto desde Git

En GitHub, abre el repositorio de ParkFlow, pulsa **Code → HTTPS** y copia la dirección. En PowerShell, cambia la URL de ejemplo por esa dirección:

```powershell
cd $HOME\Documents
git clone https://github.com/USUARIO/REPOSITORIO.git PARKFLOW-SYSTEM
cd .\PARKFLOW-SYSTEM\app\backend
```

Si el repositorio es privado, GitHub solicitará iniciar sesión o usar un token. Otra opción es descargar el ZIP desde **Code → Download ZIP**, extraerlo y abrir la carpeta `app\backend`.

Confirma que estás en la carpeta correcta:

```powershell
Test-Path .\docker-compose.parkflow.yml
Test-Path .\.env.parkflow.example
```

Ambos comandos deben devolver `True`.

## 3. Configurar el entorno local

Crea tu archivo de configuración local copiando la plantilla:

```powershell
Copy-Item .\.env.parkflow.example .\.env.parkflow
```

Genera las llaves RSA que Identity usa para firmar y validar los tokens:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\New-ParkFlowJwtKeys.ps1
```

El script guarda las llaves codificadas en `.env.parkflow`. Ese archivo es local y no se debe publicar ni enviar al repositorio. La plantilla `.env.parkflow.example` sí se puede compartir; contiene valores de ejemplo para desarrollo local.

## 4. Iniciar ParkFlow

Desde `app\backend`, ejecuta:

```powershell
docker compose --env-file .env.parkflow -f .\docker-compose.parkflow.yml up --build -d
```

La primera ejecución puede tardar varios minutos. Docker construye Identity, Parking, Billing y el frontend, inicia PostgreSQL y ejecuta las migraciones. Para ver el estado:

```powershell
docker compose --env-file .env.parkflow -f .\docker-compose.parkflow.yml ps
```

Para consultar los registros de inicio:

```powershell
docker compose --env-file .env.parkflow -f .\docker-compose.parkflow.yml logs --tail 100
```

## 5. Abrir el sistema y crear el administrador

Abre el frontend en:

```text
http://localhost:5173
```

En una instalación nueva, crea la primera cuenta administradora desde PowerShell. Sustituye el correo y la contraseña por datos propios de prueba:

```powershell
$body = @{
  nombre = 'Administrador'
  email = 'admin@parkflow.local'
  password = 'CambiaEstaClave123'
} | ConvertTo-Json

Invoke-RestMethod -Method Post `
  -Uri 'http://localhost:8081/api/auth/register' `
  -ContentType 'application/json' `
  -Body $body
```

El registro inicial recibe el rol de administrador cuando todavía no hay usuarios. Luego inicia sesión en el frontend con ese correo y contraseña. Si la cuenta ya existe, no vuelvas a registrarla: usa las credenciales existentes.

## 6. Direcciones y puertos

| Componente | Dirección local |
| --- | --- |
| Frontend | `http://localhost:5173` |
| Parking | `http://localhost:8080` |
| Identity | `http://localhost:8081` |
| Billing | `http://localhost:8083` |
| PostgreSQL | `localhost:55433` |

El contenedor PostgreSQL aloja las tres bases (`db_parking`, `db_identity` y `db_billing`). El Compose y los servicios usan las migraciones activas de `parkflow-db-definitivo`; `parkflow-db-nuevo` se conserva como referencia y no es cargada por los servicios actuales.

## 7. Detener y volver a iniciar

Para detener los contenedores sin borrar las bases ni sus datos:

```powershell
docker compose --env-file .env.parkflow -f .\docker-compose.parkflow.yml down
```

Para volver a iniciar después:

```powershell
docker compose --env-file .env.parkflow -f .\docker-compose.parkflow.yml up -d
```

No uses `docker compose down -v` si quieres conservar los datos: la opción `-v` elimina el volumen de PostgreSQL.

## 8. Solución de problemas comunes

**Docker dice que no encuentra `.env.parkflow`.** Ejecuta los comandos desde `app\backend`, y confirma que creaste el archivo con `Copy-Item`.

**Docker no puede conectar con el daemon.** Abre Docker Desktop y espera a que termine de iniciar. Comprueba con `docker info`.

**El frontend muestra “Failed to fetch”.** Revisa que Identity, Parking y Billing estén activos con `docker compose ... ps`; consulta los registros con `docker compose ... logs identity parking billing`. El frontend debe abrirse desde `http://localhost:5173` para que coincida con los orígenes CORS locales configurados.

**El puerto ya está ocupado.** Cierra la aplicación que lo usa o cambia el puerto correspondiente en `.env.parkflow` antes de iniciar.

**Un contenedor se cierra al iniciar.** Consulta `docker compose ... logs NOMBRE_SERVICIO` (por ejemplo, `identity` o `postgres`) para ver el motivo. Si faltan las llaves JWT, vuelve a ejecutar el script de generación y luego recrea los servicios con `up --build -d`.

**Cambiaste un valor de `.env.parkflow`.** Recrea los contenedores para que reciban la configuración nueva:

```powershell
docker compose --env-file .env.parkflow -f .\docker-compose.parkflow.yml up --build -d
```

## 9. Alcance de esta instalación

Este procedimiento levanta el sistema local para desarrollo o evaluación en el mismo computador. No publica ParkFlow en Internet. Para entrar desde otro computador se necesita un despliegue accesible por red, configurar HTTPS y CORS, y proteger credenciales y bases de datos. La aplicación todavía tiene funciones descritas como pendientes en el [estado de implementación](../PARKFLOW_IMPLEMENTATION_ROADMAP.md).

## 10. Referencias del proyecto

- [README principal del backend](../README.md)
- [Estado de implementación](../PARKFLOW_IMPLEMENTATION_ROADMAP.md)
- [Guía técnica de despliegue](../docs/modelado/09_DESPLIEGUE.md)
- [Plantilla de variables locales](../.env.parkflow.example)
