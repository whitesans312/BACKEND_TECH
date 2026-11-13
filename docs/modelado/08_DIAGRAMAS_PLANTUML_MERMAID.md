# Fuentes de diagramas ParkFlow

Todos los diagramas son código fuente editable. Los ERD detallados están separados por base en `01_MODELO_DB_PARKING.md`, `02_MODELO_DB_IDENTITY.md` y `03_MODELO_DB_BILLING.md`.

## 1. Arquitectura general (Mermaid)

```mermaid
flowchart LR
  O[Operador / Administrador] --> UI[Frontend React / TypeScript]
  UI -->|REST + JWT| P[Parking Service :8080]
  UI -->|REST + JWT| I[Identity Service :8081]
  UI -->|REST + JWT| B[Billing Service :8083]
  P -->|REST idempotente| B
  I -.->|JWKS / RS256| P
  I -.->|JWKS / RS256| B
  P --> DP[(db_parking)]
  I --> DI[(db_identity)]
  B --> DB[(db_billing)]
```

## 2. Componentes internos

Los diagramas Controller→Service→Repository→Entity→Database por servicio están en `06_COMPONENTES_INTERNOS.md`. Muestran las clases presentes, las relaciones JPA observadas y los puntos JDBC/REST, sin representar como entidad las tablas que solo están en migraciones.

## 3. Comunicación de salida (Mermaid)

```mermaid
sequenceDiagram
  actor Operador
  participant UI as Frontend
  participant P as Parking
  participant DBP as db_parking
  participant B as Billing
  participant DBB as db_billing
  Operador->>UI: Solicitar liquidación
  UI->>P: POST /api/sesiones/{ticket}/salida + Idempotency-Key
  P->>DBP: Leer/bloquear sesión activa
  P->>B: GET /internal/v1/liquidaciones/by-idempotency-key/{key}
  alt No hay liquidación (404)
    P->>B: POST /internal/v1/liquidaciones + token + clave + snapshot de sesión
    B->>DBB: Guardar liquidación y detalle
    B-->>P: Estado e importe
  else Reintento
    B-->>P: Liquidación existente
  end
  P-->>UI: Pendiente (202) o salida confirmada (200)
  Operador->>UI: Confirma efectivo recibido
  UI->>B: POST /internal/v1/pagos/{id}/resolver + sesión de caja
  B->>DBB: Transacción local: pago, liquidación, movimiento, comprobante
  B-->>UI: Pago y recibo
  UI->>P: Reintentar POST salida con misma clave
  P->>B: Recuperar liquidación
  B-->>P: PAGADA
  P->>DBP: Cerrar sesión y liberar espacio
```

`SesionService.solicitarSalida` mantiene anotación `@Transactional` durante llamadas Billing; el diagrama muestra el orden lógico y no oculta esa diferencia de implementación.

## 4. Diagrama de despliegue (Mermaid)

```mermaid
flowchart TB
  subgraph Host[Equipo de desarrollo]
    Browser[Browser :5173]
    subgraph Compose[Docker Compose parkflow-local]
      FE[frontend container :80]
      I[identity container :8081]
      P[parking container :8080]
      B[billing container :8083]
      PG[postgres:16.15 container :5432]
      DBI[(db_identity)]
      DBP[(db_parking)]
      DBB[(db_billing)]
      I --> DBI
      P --> DBP
      B --> DBB
      I --> PG
      P --> PG
      B --> PG
      P --> B
      I -. JWKS .-> P
      I -. JWKS .-> B
    end
    Browser -->|host 5173→80| FE
    Browser -->|host 8081| I
    Browser -->|host 8080| P
    Browser -->|host 8083| B
    HostPG[host 55433→container 5432] --> PG
  end
```

## 5. ERD: reglas de lectura

Los tres ERD son locales a cada base. Las referencias UUID externas no se muestran como relaciones FK; están etiquetadas en los documentos respectivos. Para regenerar diagramas Mermaid, copiar el bloque `mermaid` a un renderer compatible. No se requiere herramienta de generación para alterar el modelo fuente.
