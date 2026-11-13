# Componentes internos de los microservicios

## Convenciones

Los diagramas resumen dependencias de código encontradas en `services/*/src/main/java`. No implican que todas las tablas migradas tengan entidades. Los componentes entre corchetes se explican abajo; los controladores HTTP llaman servicios de aplicación, que llaman repositorios o JDBC.

## Parking Service

```mermaid
flowchart TB
  subgraph API[API / Controllers]
    SetupC[ParqueaderoSetupController]
    PisoC[PisoController]
    ZonaC[ZonaController]
    EspacioC[EspacioController]
    SesionC[SesionController]
    HealthC[HealthController]
  end
  subgraph App[Application]
    SetupS[ParqueaderoSetupService]
    PisoS[PisoService]
    ZonaS[ZonaService]
    EspacioS[EspacioService]
    SesionS[SesionService]
  end
  subgraph Repo[Repositories / SQL]
    PisoR[PisoRepository]
    ZonaR[ZonaRepository]
    EspacioR[EspacioRepository + JDBC]
    SesionR[SesionParqueoRepository + JDBC]
  end
  subgraph Domain[Entidades JPA]
    Piso[Piso]
    Zona[Zona]
    Espacio[Espacio]
    Sesion[SesionParqueo]
  end
  DB[(db_parking)]
  Bill[Billing REST Client]
  SetupC --> SetupS
  SetupS --> PisoS
  SetupS --> ZonaS
  SetupS --> EspacioS
  PisoC --> PisoS --> PisoR --> Piso --> DB
  ZonaC --> ZonaS --> ZonaR --> Zona --> DB
  EspacioC --> EspacioS --> EspacioR --> Espacio --> DB
  SesionC --> SesionS --> SesionR --> Sesion --> DB
  SesionS --> EspacioR
  SesionS --> Bill
```

Módulos/repositorios verificados: pisos, zonas, espacios y sesiones. SesionService también ejecuta SQL para secuencia de tiquete/eventos y usa `RestClient` hacia Billing. Configuración compuesta coordina creación de pisos, zonas y espacios.

## Identity Service

```mermaid
flowchart TB
  subgraph API[API / Controllers]
    AuthC[AuthController]
    UserC[UsuarioController]
    RoleC[RolController]
    JwksC[JwksController]
  end
  AppS[IdentityService]
  UserR[UsuarioRepository]
  RoleR[RolRepository]
  UserE[Usuario JPA]
  RoleE[Rol JPA]
  Sec[SecurityConfiguration + TokenIssuer + claves RS256]
  DB[(db_identity)]
  AuthC --> AppS
  UserC --> AppS
  RoleC --> AppS
  JwksC --> Sec
  AppS --> UserR --> UserE --> DB
  AppS --> RoleR --> RoleE --> DB
  AppS --> Sec
```

`IdentityService` implementa operaciones de usuario/rol, login/registro, bootstrap y emisión de tokens. Hay JPA para `Usuario`, `Rol` y la relación Many-to-Many. Las tablas de autorización/auditoría de DDL no tienen entidades JPA en el código listado; no se debe asumir que están expuestas en APIs.

## Billing Service

```mermaid
flowchart TB
  subgraph API[API / Controllers]
    TarifaC[TarifaController]
    LiquidacionC[LiquidacionController]
    PagoC[ResolucionPagoController]
    CajaC[CajaController]
    ConsultaC[ConsultaBillingController]
    HealthC[BillingHealthController]
  end
  subgraph App[Application]
    TarifaS[TarifaService]
    LiquidacionS[LiquidacionService]
    Calc[CalculadoraLiquidacion]
    PagoS[ResolucionPagoService]
    CajaS[CajaService]
    ConsultaS[ConsultaPagoService]
  end
  Repo[Repositories + JDBC / transacciones]
  Ent[JPA: Tarifa, Liquidacion, DetalleLiquidacion, Pago, Comprobante, Caja, SesionCaja]
  DB[(db_billing)]
  TarifaC --> TarifaS
  LiquidacionC --> LiquidacionS --> Calc
  PagoC --> PagoS
  CajaC --> CajaS
  ConsultaC --> ConsultaS
  TarifaS --> Repo
  LiquidacionS --> Repo
  PagoS --> Repo
  CajaS --> Repo
  ConsultaS --> Repo
  Repo --> Ent --> DB
```

`LiquidacionService` usa hash de solicitud, `TransactionTemplate`, repositorios y JDBC para preservar idempotencia y snapshots. `ResolucionPagoService` realiza pago, transición de liquidación, movimiento de caja y comprobante dentro de transacción local. Las tablas `parametros_facturacion`, `cargos_abandono`, `eventos_cargo_abandono` y `anulaciones_liquidacion` están en DDL, pero no tienen entidades JPA encontradas en el listado.

## Cobertura JPA resumida

| Servicio | Entidades JPA actuales | Tablas DDL de esa base | Tablas no mapeadas por entidad JPA observada |
|---|---:|---:|---:|
| Parking | 4 | 14 | 10 |
| Identity | 2 | 7 | 4 |
| Billing | 7 | 12 | 5 |

La tabla `usuario_roles` es tabla de unión JPA, pero no entidad independiente; por eso se cuentan 3 tablas mapeadas de Identity (2 entidades + join table) y 4 no mapeadas. El conteo de entidades no equivale a cobertura funcional. Los números de tablas y mapeos se contrastaron con migraciones y anotaciones `@Entity`/`@JoinTable`.
