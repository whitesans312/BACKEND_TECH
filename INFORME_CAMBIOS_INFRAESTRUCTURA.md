# Informe Técnico de Cambios e Infraestructura — ParkFlow (`BACKEND_TECH`)

Este documento detalla al pie de la letra los archivos creados, los archivos modificados, el mapa de red/puertos/rutas Traefik/variables de entorno y la guía de verificación paso a paso para el despliegue local y entrega académica de **ParkFlow**.

---

## 1. Inventario de Archivos Creados

| Ruta exacta (relativa a `BACKEND_TECH`) | Propósito técnico |
| --- | --- |
| `services/identity-service/src/main/resources/application-docker.yml` | Define el perfil Spring `docker` para `identity-service`: escucha en `0.0.0.0:8081`, conecta a `jdbc:postgresql://postgres:5432/db_identity`, habilita Flyway (`V1`–`V3`) con el rol `identity_migrator` y expone `/actuator/health` con `show-details: always` y probes (`liveness`/`readiness`). |
| `services/parking-service/src/main/resources/application-docker.yml` | Define el perfil Spring `docker` para `parking-service`: escucha en `0.0.0.0:8080`, conecta a `jdbc:postgresql://postgres:5432/db_parking`, habilita Flyway (`V1`–`V5`, incluyendo `V5__pisos_admite_sotanos.sql`) con `parking_migrator`, configura las URLs internas de contenedores (`IDENTITY_JWKS_URI=http://identity-service:8081/.well-known/jwks.json`, `BILLING_SERVICE_URL=http://billing-service:8083`) y expone `/actuator/health`. |
| `services/billing-service/src/main/resources/application-docker.yml` | Define el perfil Spring `docker` para `billing-service`: escucha en `0.0.0.0:8083`, conecta a `jdbc:postgresql://postgres:5432/db_billing`, habilita Flyway (`V1`–`V4`) con `billing_migrator`, configura `IDENTITY_JWKS_URI=http://identity-service:8081/.well-known/jwks.json` y expone `/actuator/health`. |
| `infra/postgres/init-databases.sql` | Script SQL idempotente montado en `/docker-entrypoint-initdb.d/01-init-databases.sql` del contenedor PostgreSQL 16. Crea de forma segura los roles aislados (`identity_migrator`, `identity_app`, `parking_migrator`, `parking_app`, `billing_migrator`, `billing_app`) y las tres bases de datos independientes (`db_identity`, `db_parking`, `db_billing`), revocando acceso `PUBLIC` y otorgando `CONNECT` solo a los roles de cada servicio. |
| `docker-compose.yml` | Orquestación principal en la raíz del proyecto con la red `parkflow-net`, volumen persistente `parkflow_pg_data`, **Traefik** (HTTP `:80` y dashboard `:8090`), **PostgreSQL 16** (`55433:5432` con `pg_isready`), y los 3 microservicios (`identity-service`, `billing-service`, `parking-service`) con `depends_on` (`condition: service_healthy`), `healthcheck` sobre `/actuator/health` y etiquetas de enrutamiento Traefik. *(Nota: usa `image: traefik:v3` —resuelto a `3.8.0`— debido a que Docker Engine `29.9.3` exige API Docker mínima `1.40`, mientras que `traefik:v3.1` realiza llamadas `v1.24` rechazadas por el daemon).* |
| `api.http` | Suite de pruebas End-to-End compatible con **REST Client** de VS Code apuntando a `http://localhost` (Traefik). Incluye variables dinámicas (`@token`, `@pisoId`, `@espacioId`, `@ticket`, `@liquidacionId`, `@pagoId`, `@cajaId`, `@sesionCajaId`, `@reservaId`) y cubre: health checks, JWKS, login, creación de topología, entrada con `Idempotency-Key` + reintento idempotente + rechazo de duplicado activo (`409`), salida gratuita en gracia (15 min), flujo completo de liquidación facturable + caja + pago + cierre, y flujo de reservaciones anticipadas (`V6`: creación, reintento idempotente, rechazo por traslape `409`, check-in a sesión activa y cancelación). |
| `parkflow-db-definitivo/parking/V6__parking_reservas.sql` | Migración Flyway V6 de `parking-service` (Bloque 2): crea la secuencia `seq_reserva`, la tabla `reservas` (`ACTIVA`, `CONVERTIDA`, `CANCELADA`, `EXPIRADA`), restricciones de integridad y ventana (`fecha_fin > fecha_inicio`), índice único parcial `uq_reserva_placa_activa`, índice único `uq_reserva_sesion_convertida`, índice de traslapes `idx_reservas_espacio_estado_ventana` y permisos para `parking_app`. |
| `services/parking-service/src/main/java/com/parkflow/reservas/...` | Módulo completo de reservaciones anticipadas en `parking-service`: `EstadoReserva`, `Reserva`, `ReservaRepository`, DTOs (`CrearReservaRequest`, `CancelarReservaRequest`, `ReservaResponse`, `ReservaCheckInResponse`), `ReservaService` (con bloqueo pesimista sobre `espacios` e idempotencia) y `ReservaController` (`/api/v1/reservas`). |
| `services/parking-service/src/test/java/com/parkflow/reservas/...` | Pruebas unitarias y MVC del módulo de reservas: `ReservaServiceTest` (4 tests) y `ReservaControllerTest` (1 test). |
| `Makefile` | Automatización de ciclo de vida del stack con los targets `help`, `up`, `down`, `build`, `restart`, `logs`, `ps`, `health` (incluye verificación de Frontend `:5173`), `db-check` (consulta `\l`, `\dt` e historial de `flyway_schema_history` en las 3 bases) y `clean`. |
| `INFORME_CAMBIOS_INFRAESTRUCTURA.md` | Presente informe técnico de entrega con inventario, bitácora detallada, matriz de red/entorno y guía de validación. |

*(Adicionalmente, en el entorno local existe `.env.parkflow` —ignorado por `.gitignore` y generado desde `.env.parkflow.example` mediante `scripts/New-ParkFlowJwtKeys.ps1`— con el par de llaves RSA PKCS#8 / X.509 en Base64 para firma RS256).*

---

## 2. Bitácora Exacta de Archivos Modificados

### A. Dependencias Maven (`pom.xml`) — HT-07
1. **`services/identity-service/pom.xml`**
   - **Cambio exacto:** Se agregó `<dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-actuator</artifactId></dependency>`.
   - **Justificación (HT-07):** Ninguno de los 3 microservicios contaba con Actuator. Se requiere para exponer `/actuator/health` con probes y detalles de conexión a PostgreSQL para los `healthcheck` de Docker Compose.
2. **`services/parking-service/pom.xml`**
   - **Cambio exacto:** Se agregó `spring-boot-starter-actuator` en `<dependencies>`.
   - **Justificación (HT-07):** Habilita `/actuator/health` para que Docker Compose valide salud antes de enrutar tráfico en Traefik.
3. **`services/billing-service/pom.xml`**
   - **Cambio exacto:** Se agregó `spring-boot-starter-actuator` en `<dependencies>`.
   - **Justificación (HT-07):** Habilita `/actuator/health` para que `parking-service` pueda depender de `billing-service: service_healthy`.

### B. Seguridad Spring Security (`SecurityConfiguration` / `ParkflowSecurityConfiguration` / `BillingSecurityConfiguration`) — HT-07
1. **`services/identity-service/src/main/java/com/parkflow/identity/security/SecurityConfiguration.java`**
   - **Cambio exacto:** En `identityFilterChain`, se amplió `.requestMatchers("/api/auth/register","/api/auth/login","/.well-known/jwks.json", "/actuator/health","/actuator/health/**").permitAll()`.
   - **Justificación (HT-07):** La regla final `.anyRequest().denyAll()` bloqueaba con `403 Forbidden` las peticiones a `/actuator/health`. Con esto quedan públicos tanto `/.well-known/jwks.json` como `/actuator/health`.
2. **`services/parking-service/src/main/java/com/parkflow/security/ParkflowSecurityConfiguration.java`**
   - **Cambio exacto:** En `parkingSecurityFilterChain`, se amplió `.requestMatchers(HttpMethod.GET,"/api/v1/health","/actuator/health","/actuator/health/**").permitAll()`.
   - **Justificación (HT-07):** Sin esta excepción, `.anyRequest().authenticated()` devolvía `401 Unauthorized` al health check nativo de Docker y a Traefik.
3. **`services/billing-service/src/main/java/com/parkflow/billing/security/BillingSecurityConfiguration.java`**
   - **Cambio exacto:** En `billingSecurityFilterChain`, se amplió `.requestMatchers(HttpMethod.GET,"/api/billing/health","/actuator/health","/actuator/health/**").permitAll()`.
   - **Justificación (HT-07):** Permite que el contenedor y Traefik consulten `/actuator/health` sin token Bearer.

### C. Propiedades de Aplicación (`application.properties`) — HT-07
1. **`services/identity-service/src/main/resources/application.properties`**
2. **`services/parking-service/src/main/resources/application.properties`**
3. **`services/billing-service/src/main/resources/application.properties`**
   - **Cambio exacto:** Se añadieron en los tres archivos:
     - `management.endpoints.web.exposure.include=health`
     - `management.endpoint.health.show-details=always`
     - `management.endpoint.health.probes.enabled=true`
   - **Justificación (HT-07):** Garantiza que `/actuator/health` esté expuesto y muestre los componentes detallados (`db`, `diskSpace`, `livenessState`, `readinessState`, `ping`) tanto en el perfil por defecto como en el perfil `docker`.

### D. Dockerfiles Multi-Stage
1. **`services/identity-service/Dockerfile`**
2. **`services/parking-service/Dockerfile`**
3. **`services/billing-service/Dockerfile`**
   - **Cambio exacto:**
     - Etapa `build`: cambiada a `FROM maven:3.9-eclipse-temurin-21 AS build`; copia primero `pom.xml` y pre-resuelve dependencias (`mvn ... dependency:resolve dependency:resolve-plugins || true`) para aprovechar caché de capas Docker antes de copiar `src` y `parkflow-db-definitivo/<servicio>` y ejecutar `mvn -B -DskipTests package`.
     - Etapa runtime: cambiada de `eclipse-temurin:21-jre` a `FROM eclipse-temurin:21-jre-alpine` con `RUN apk add --no-cache curl`.
   - **Justificación:** Cumple el estándar solicitado (`maven:3.9-eclipse-temurin-21` + `eclipse-temurin:21-jre-alpine`), reduce el tamaño final de imagen y provee `curl` para ejecutar el `healthcheck` nativo de Docker contra `/actuator/health`.

### E. Concurrencia, Transaccionalidad y Prevención de Duplicados (`SesionService.java` y pruebas) — HT-05 y HT-10
1. **`services/parking-service/src/main/java/com/parkflow/sesiones/application/SesionService.java`**
   - **Cambio exacto:**
     - Se eliminó `@Transactional` del método `solicitarSalida(String ticket, UUID operationId, Jwt jwt)`, que anteriormente abría una transacción y adquiría un bloqueo pesimista `findByNumeroTicketForUpdate(ticket)` (`SELECT ... FOR UPDATE`) **antes** de realizar las llamadas HTTP síncronas (`GET` / `POST`) hacia `billing-service`.
     - Se inyectó `PlatformTransactionManager` vía constructor `@Autowired` (manteniendo el constructor package-private de 4 parámetros para compatibilidad con `SesionServiceTest` y añadiendo un constructor package-private con `RestClient.Builder` para pruebas con `MockRestServiceServer`) y se configuraron timeouts en `SimpleClientHttpRequestFactory` (`5s` de conexión y `10s` de lectura).
     - Se dividió `solicitarSalida` en 3 fases:
       1. `executeReadOnly`: lectura sin bloqueo (`sesiones.findByNumeroTicket(ticket)`), validación de estado y captura de `SalidaSnapshot` (incluyendo `SesionResponse` antes de cerrar la sesión Hibernate).
       2. `resolveOrCreateSettlement`: llamadas HTTP a `billing-service` **fuera de toda transacción o bloqueo de PostgreSQL**.
       3. `executeWrite`: solo si la liquidación retorna `PAGADA` o `GRATUITA`, abre transacción corta con `sesiones.findByNumeroTicketForUpdate(ticket)`, re-verifica idempotencia/estado ante posibles carreras concurrentes, cierra la sesión y ejecuta `sesiones.saveAndFlush(locked)`.
   - **Revisión verificada de HT-10 y concurrencia:**
     - `GET /api/v1/espacios` (`EspacioService.findAll` -> `EspacioRepository.findAvailability`) evalúa en SQL `habilitado`, `p.activo AND z.activa`, `ocupado` (`EXISTS ... sesiones_parqueo WHERE estado = 'ACTIVA'`) y calcula `disponible`.
     - `POST /api/sesiones/entradas` (`SesionService.registrarEntrada`) normaliza la placa, valida idempotencia por `Idempotency-Key` (`findByClaveIdempotencia` + índice único `uq_sesiones_clave_idempotencia`), verifica que no exista sesión activa para la placa (`existsByPlacaActualAndEstado` + índice único parcial `uq_sesion_placa_activa`), bloquea la fila de `espacios` con `PESSIMISTIC_WRITE` (`findByIdForUpdate`), valida compatibilidad de tipo, habilitación de espacio/zona/piso y ausencia de sesión activa en el espacio (`uq_sesion_espacio_activo`).
     - `POST /internal/v1/liquidaciones` (`LiquidacionService.crear`) utiliza `TransactionTemplate(PROPAGATION_REQUIRES_NEW)`, validación de `hashSolicitud`, `clave_idempotencia` única e índice parcial `uq_liquidacion_pendiente_sesion` con captura de `DataIntegrityViolationException`.
     - `POST /internal/v1/pagos/{id}/resolver` (`ResolucionPagoService.aprobar`) bloquea con `pagos.lockById(id)` y `sesionesCaja.lockById(request.sesionCajaId())`, valida idempotencia si ya fue aprobado en la misma sesión de caja y respeta los índices únicos `uq_pago_aprobado_liquidacion` / `uq_pago_pendiente_liquidacion`.
2. **`services/parking-service/src/test/java/com/parkflow/sesiones/application/SesionServiceTest.java`**
   - **Cambio exacto:** Se añadió el test unitario `solicitarSalidaDoesNotLockRowBeforeBillingHttpCallWhenPending` usando `MockRestServiceServer`.
   - **Justificación (HT-05):** Comprueba automáticamente que `solicitarSalida` consulta por `findByNumeroTicket` y nunca invoca `findByNumeroTicketForUpdate` durante/antes de la llamada HTTP cuando la liquidación aún está `PENDIENTE`.

*(Nota de referencia: en una intervención previa de arranque con el frontend, `docker-compose.parkflow.yml` línea 106 se actualizó de `context: ../frontend` a `context: ../FRONT_TECH`; el nuevo archivo principal para despliegue con Traefik es `docker-compose.yml`).*

### F. Bloque 1 y Bloque 2 — Integración Frontend, Limpieza y Módulo de Reservaciones Anticipadas
1. **Integración Frontend en `BACKEND_TECH/docker-compose.yml` y `BACKEND_TECH/Makefile`**
   - **Cambio exacto:** Se añadió el servicio `frontend` (`context: ../FRONT_TECH`, `VITE_IDENTITY_URL=http://localhost`, `VITE_PARKING_URL=http://localhost`, `VITE_BILLING_URL=http://localhost`, puerto `5173:80`, `healthcheck` con `wget`, esperando a los 3 microservicios `service_healthy`), y se añadió la verificación de `http://localhost:5173` en el target `health` del `Makefile`.
   - **Justificación:** Unifica toda la pila (Frontend + Gateway Traefik + 3 microservicios + PostgreSQL) bajo un único `docker-compose.yml`.
2. **Limpieza y actualización de `FRONT_TECH`**
   - **`FRONT_TECH/src/ParkFlowApp.tsx` y `FRONT_TECH/src/ParkFlowConfig.css`:** Actualizados los valores por defecto de API a `http://localhost` (Traefik) y añadidos el card rápido, modal, tabla, filtros y acciones (`Check-in` y `Cancelar`) para **Reservas anticipadas**, además de mostrar el estado `Reservado` en la tabla administrativa de espacios.
   - **`FRONT_TECH/index.html`, `FRONT_TECH/vite.config.ts`, `FRONT_TECH/src/index.css`:** Título actualizado (`ParkFlow · Control de Parqueadero`), puerto dev `5173`, CSS base limpio y eliminación de carpetas/archivos heredados ajenos a ParkFlow (`src/components/`, `src/pages/`, `src/services/`, `src/types/`, `src/utils/`, `src/config/`, `src/assets/`, `src/App.css`).
3. **Soporte de Reservaciones en `parking-service` y Traefik**
   - **`BACKEND_TECH/docker-compose.yml`:** Agregado `PathPrefix('/api/v1/reservas')` a la regla del router `parking` en Traefik.
   - **`services/parking-service/src/main/java/com/parkflow/security/ParkflowSecurityConfiguration.java`:** Permitido `/api/v1/reservas/**` para roles `ADMINISTRADOR` y `OPERADOR`.
   - **`services/parking-service/src/main/java/com/parkflow/espacios/...` (`EspacioAvailabilityView.java`, `EspacioRepository.java`, `EspacioResponse.java`):** Agregado campo `reservado` y validación `!reservado` dentro de `disponible` (preservando constructores previos de compatibilidad).
   - **`services/parking-service/src/main/java/com/parkflow/sesiones/application/SesionService.java`:** En `registrarEntrada`, valida `espacios.existsCurrentActiveReservation(espacio.getId(), Instant.now())` para impedir entradas directas en espacios con reserva vigente en ese horario.
   - **`services/parking-service/src/main/java/com/parkflow/shared/error/ApiExceptionHandler.java`:** Añadido manejo `400 Bad Request` para `IllegalArgumentException` y `MissingRequestHeaderException`.

---

## 3. Mapa de Puertos, Rutas Traefik y Variables de Entorno

### 3.1 Contenedores, Red y Exposición de Puertos

| Servicio Compose | Contenedor | Red | Puerto interno | Puerto externo (Host) | Dependencias (`condition`) |
| --- | --- | --- | --- | --- | --- |
| `traefik` | `parkflow-traefik` | `parkflow-net` | `80`, `8080` | `80:80` (HTTP), `8090:8080` (Dashboard) | — |
| `postgres` | `parkflow-postgres` | `parkflow-net` | `5432` | `55433:5432` | — |
| `identity-service` | `parkflow-identity` | `parkflow-net` | `8081` | Solo vía Traefik `:80` | `postgres` (`service_healthy`) |
| `billing-service` | `parkflow-billing` | `parkflow-net` | `8083` | Solo vía Traefik `:80` | `postgres` (`service_healthy`), `identity-service` (`service_healthy`) |
| `parking-service` | `parkflow-parking` | `parkflow-net` | `8080` | Solo vía Traefik `:80` | `postgres` (`service_healthy`), `identity-service` (`service_healthy`), `billing-service` (`service_healthy`) |

### 3.2 Tabla de Enrutamiento Traefik (`http://localhost`)

| Router Traefik | Regla de ruta (`entrypoints=web`) | Destino interno | Notas |
| --- | --- | --- | --- |
| `identity` | `PathPrefix('/api/auth') \|\| PathPrefix('/api/users') \|\| PathPrefix('/api/roles') \|\| Path('/.well-known/jwks.json')` | `identity-service:8081` | Auth, usuarios, roles y JWKS público RS256. |
| `identity-health` | `Path('/health/identity')` | `identity-service:8081/actuator/health` | Middleware `replacepath.path=/actuator/health`. |
| `parking` | `PathPrefix('/api/v1/parqueadero') \|\| PathPrefix('/api/v1/pisos') \|\| PathPrefix('/api/zonas') \|\| PathPrefix('/api/v1/espacios') \|\| PathPrefix('/api/v1/reservas') \|\| PathPrefix('/api/sesiones') \|\| Path('/api/v1/health')` | `parking-service:8080` | Topología, disponibilidad, reservas, entradas/salidas y health custom. |
| `parking-health` | `Path('/health/parking')` | `parking-service:8080/actuator/health` | Middleware `replacepath.path=/actuator/health`. |
| `billing` | `PathPrefix('/api/billing') \|\| PathPrefix('/internal/v1')` | `billing-service:8083` | Tarifas, cajas, comprobantes, liquidaciones y pagos. |
| `billing-health` | `Path('/health/billing')` | `billing-service:8083/actuator/health` | Middleware `replacepath.path=/actuator/health`. |

### 3.3 Bases de Datos y Variables de Entorno Clave

| Servicio | Base de datos / URL JDBC en `docker` | Migraciones Flyway | Variables clave de comunicación y seguridad |
| --- | --- | --- | --- |
| `identity-service` | `jdbc:postgresql://postgres:5432/db_identity` (`identity_app` / `identity_migrator`) | `V1`–`V3` | `SPRING_PROFILES_ACTIVE=docker`, `IDENTITY_FLYWAY_ENABLED=true`, `IDENTITY_JWT_ISSUER=https://identity.parkflow.local`, `IDENTITY_JWT_AUDIENCE=parkflow-api`, `IDENTITY_JWT_PRIVATE_KEY_B64`, `IDENTITY_JWT_PUBLIC_KEY_B64` (cargadas desde `.env.parkflow`). |
| `parking-service` | `jdbc:postgresql://postgres:5432/db_parking` (`parking_app` / `parking_migrator`) | `V1`–`V6` | `SPRING_PROFILES_ACTIVE=docker`, `PARKING_FLYWAY_ENABLED=true`, `PARKING_SERVER_ADDRESS=0.0.0.0`, `IDENTITY_JWKS_URI=http://identity-service:8081/.well-known/jwks.json`, `BILLING_SERVICE_URL=http://billing-service:8083`. |
| `billing-service` | `jdbc:postgresql://postgres:5432/db_billing` (`billing_app` / `billing_migrator`) | `V1`–`V4` | `SPRING_PROFILES_ACTIVE=docker`, `BILLING_FLYWAY_ENABLED=true`, `BILLING_SERVER_ADDRESS=0.0.0.0`, `IDENTITY_JWKS_URI=http://identity-service:8081/.well-known/jwks.json`, `PARKFLOW_GRACE_MINUTES=15`. |
| `frontend` | — (React + Vite servido en Nginx) | — | `VITE_IDENTITY_URL=http://localhost`, `VITE_PARKING_URL=http://localhost`, `VITE_BILLING_URL=http://localhost`, puerto `5173:80`. |

---

## 4. Guía de Verificación y Evidencias

### 4.1 Validación por Terminal (desde `BACKEND_TECH/`)

1. **Validar sintaxis YAML de Compose:**
   ```powershell
   docker compose --env-file .env.parkflow -f docker-compose.yml config --quiet
   ```
2. **Compilar código y pruebas (`mvn test-compile` y `SesionServiceTest`):**
   ```powershell
   $ws = (Resolve-Path .).Path
   docker run --rm -v "${ws}:/workspace" -w /workspace maven:3.9-eclipse-temurin-21 sh -c "mvn -f services/identity-service/pom.xml -B test-compile && mvn -f services/billing-service/pom.xml -B test-compile && mvn -f services/parking-service/pom.xml -B test-compile && mvn -f services/parking-service/pom.xml -B -Dtest=SesionServiceTest test"
   ```
3. **Construir imágenes y levantar todo el stack:**
   ```powershell
   docker compose --env-file .env.parkflow -f docker-compose.yml up -d --build
   # (o con Makefile: make up)
   ```
4. **Verificar contenedores saludables (`service_healthy`):**
   ```powershell
   docker compose --env-file .env.parkflow -f docker-compose.yml ps
   # (o: make ps)
   ```
   *(Captura recomendada 1: todos los servicios `Up (healthy)` y Traefik activo).*
5. **Verificar Health Checks y JWKS a través de Traefik:**
   ```powershell
   curl.exe -fsS http://localhost/health/identity
   curl.exe -fsS http://localhost/health/parking
   curl.exe -fsS http://localhost/health/billing
   curl.exe -fsS http://localhost/.well-known/jwks.json
   # (o: make health)
   ```
   *(Captura recomendada 2: respuestas JSON `"status":"UP"` con detalles de `db: PostgreSQL` y el dashboard de Traefik en `http://localhost:8090/dashboard/`).*
6. **Verificar bases aisladas y migraciones Flyway (`V1`–`V3`, `V1`–`V5`, `V1`–`V4`):**
   ```powershell
   docker compose --env-file .env.parkflow -f docker-compose.yml exec -T postgres psql -U parkflow_admin -d db_identity -c "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"
   docker compose --env-file .env.parkflow -f docker-compose.yml exec -T postgres psql -U parkflow_admin -d db_parking -c "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"
   docker compose --env-file .env.parkflow -f docker-compose.yml exec -T postgres psql -U parkflow_admin -d db_billing -c "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"
   # (o: make db-check)
   ```
   *(Captura recomendada 3: tablas `flyway_schema_history` mostrando `5 | pisos admite sotanos | t` en `db_parking`).*

### 4.2 Validación End-to-End en `api.http` (REST Client VS Code)

Ejecutar las peticiones de [api.http](api.http) en estricto orden secuencial:
1. **Sección 1 (1.1 – 1.5):** Confirmar `200 OK` en `/health/identity`, `/health/parking`, `/health/billing` y `/.well-known/jwks.json`.
2. **Sección 2 (2.1 – 2.3):** Ejecutar registro inicial (si la BD está nueva) y `login` (`POST /api/auth/login`) para poblar automáticamente `@token`.
3. **Sección 3 (3.1 – 3.5):** Crear piso `P1` (`@pisoId`), zona `Z-A` (`@zonaId`), espacio `C-01` (`@espacioId`), espacio `M-01` (`@espacioMotoId`) y verificar `GET /api/v1/espacios?pisoId={{pisoId}}` con `disponible: true`.
4. **Sección 4 (4.1 – 4.4):** Registrar entrada de `ABC123` (`@ticket`), reintentar **4.2** con la **misma `Idempotency-Key`** confirmando que devuelve el **mismo `@ticket`** sin duplicar, ejecutar **4.3** con otra clave confirmando `409 Conflict`, y verificar en **4.4** que `C-01` queda `ocupado: true`, `disponible: false`. *(Captura recomendada 4: evidencia de idempotencia HT-10).*
5. **Sección 5 (5.1):** Solicitar salida dentro del periodo de gracia (`POST /api/sesiones/{{ticket}}/salida`) verificando `estadoLiquidacion: "GRATUITA"` y `salidaConfirmada: true`.
6. **Sección 6 (6.1 – 6.8):** Registrar entrada de moto `MOT99A`, crear liquidación facturable (`@liquidacionId`, `@pagoId`, total `$7.000` COP), crear y abrir caja (`@cajaId`, `@sesionCajaId`), aprobar pago en efectivo (`POST /internal/v1/pagos/{{pagoId}}/resolver`), confirmar salida en Parking con la misma `Idempotency-Key` (`salidaConfirmada: true`) y verificar en **6.8** que ambos espacios quedan nuevamente `disponible: true`. *(Captura recomendada 5: liquidación `PAGADA` con comprobante y liberación de espacios).*
7. **Sección 7 (7.1 – 7.7):** Crear reserva anticipada (`POST /api/v1/reservas`), reintentar con la misma `Idempotency-Key` (sin duplicar), comprobar rechazo `409` por traslape horario, listar reservas activas, ejecutar `check-in` (`POST /api/v1/reservas/{{reservaId}}/check-in`) y cancelar una segunda reserva (`PATCH /api/v1/reservas/{{reservaMotoId}}/cancelar`).

---

## 5. Pendientes Asignados al Resto del Grupo (Bloques 3 y 4)

Para completar la entrega grupal quedan únicamente los siguientes bloques para el resto del equipo:

- **Bloque 3 — Flujos operativos adicionales (`PARKFLOW_IMPLEMENTATION_ROADMAP.md`):**
  1. Traslados entre espacios, mantenimientos de espacio y anulación de sesiones en `parking-service`.
  2. Salidas excepcionales y casos de abandono con consumo de autorizaciones de un solo uso entre `identity-service`, `parking-service` y `billing-service`.
  3. Definición comercial y activación de regla para recargo nocturno (`RECARGO_NOCTURNO_HABILITADO`).
  4. Egresos/ajustes de caja y arqueo por denominaciones en `billing-service`.
- **Bloque 4 — Pruebas integrales, evidencias y actualización documental:**
  1. Correr `mvn test` completo (incluyendo suites Testcontainers) en los tres microservicios y añadir pruebas de concurrencia paralela.
  2. Tomar capturas de `make ps`, `make health`, `make db-check`, Traefik dashboard (`http://localhost:8090`), interfaz web (`http://localhost:5173`) y `api.http`.
  3. Actualizar los documentos históricos `parkflow-db-definitivo/docs/01`–`09` y `MICROSERVICES_CATALOG.md` al estado actual.
