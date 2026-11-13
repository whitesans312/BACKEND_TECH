# ParkFlow Identity Service

Servicio independiente para usuarios, roles, autenticación y emisión de JWT. Es el único propietario de `usuarios`, `roles` y `usuario_roles` en `db_identity`.

## Persistencia

Las migraciones oficiales se mantienen en `../../parkflow-db/identity-service/src/main/resources/db/migration` y Maven las empaqueta en `db/migration`; no hay una copia de SQL en este servicio. Hibernate usa `ddl-auto=validate`. Flyway queda deshabilitado por defecto y puede habilitarse con `IDENTITY_FLYWAY_ENABLED=true` únicamente en el proceso de integración/preparación de base.

V1 crea los roles `ADMINISTRADOR` y `OPERADOR`. El registro público crea usuarios `OPERADOR`; no acepta roles del solicitante. La asignación de `ADMINISTRADOR` solo está disponible para un administrador autenticado.

## Configuración

Configura `IDENTITY_DB_HOST`, `IDENTITY_DB_PORT`, `IDENTITY_DB_NAME`, `IDENTITY_DB_USERNAME` e `IDENTITY_DB_PASSWORD` para la base del servicio. No se debe compartir el usuario de base con otro microservicio.

El proceso requiere un par RSA externo en DER codificado en Base64: clave privada PKCS#8 y clave pública X.509. Configura `IDENTITY_JWT_PRIVATE_KEY_B64` y `IDENTITY_JWT_PUBLIC_KEY_B64` mediante un gestor de secretos; no las guardes en Git ni en imágenes. Generación local de ejemplo:

```powershell
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:3072 -out identity-private.pem
openssl pkey -in identity-private.pem -pubout -out identity-public.pem
[Convert]::ToBase64String([IO.File]::ReadAllBytes('identity-private.der'))
```

Para la clave privada DER, exporta PKCS#8 desde PEM con `openssl pkcs8 -topk8 -nocrypt -in identity-private.pem -outform DER -out identity-private.der`; para la pública X.509 usa `openssl pkey -in identity-private.pem -pubout -outform DER -out identity-public.der`. Codifica ambos archivos DER con `ToBase64String` y carga los resultados como secretos de entorno.

Parámetros adicionales: `IDENTITY_JWT_ISSUER` (por defecto `https://identity.parkflow.local`), `IDENTITY_JWT_AUDIENCE` (`parkflow-api`) y `IDENTITY_JWT_KEY_ID` (`parkflow-2026-01`). Los tokens duran 15 minutos, usan RS256, UUID en `sub` y roles en `roles`. Validación exige firma, emisor, audiencia y expiración.

La publicación JWKS entrega solo la clave pública. Esta versión configura una clave activa. Para rotarla sin invalidar tokens: publica una versión nueva de la clave con un `kid` nuevo después de esperar a que expiren todos los tokens emitidos por la clave anterior (15 minutos desde la última emisión), o amplía el servicio para publicar ambas claves públicas durante el solapamiento antes de cambiar el emisor. No sobrescribas una clave activa sin respetar ese periodo.

## API

Las respuestas de error usan `application/problem+json`.

| Método | Ruta | Acceso | Resultado |
|---|---|---|---|
| `POST` | `/api/auth/register` | Público | Crea un operador; `201` |
| `POST` | `/api/auth/login` | Público | JWT Bearer y usuario; `200` |
| `GET` | `/api/auth/me` | Autenticado | Usuario del `sub` validado; `200` |
| `GET` | `/.well-known/jwks.json` | Público | Clave RSA pública activa; `200` |
| `GET` | `/api/users` | ADMINISTRADOR | Lista usuarios; `200` |
| `POST` | `/api/users` | ADMINISTRADOR | Crea usuario con roles; `201` |
| `GET` | `/api/users/{id}` | ADMINISTRADOR | Consulta usuario; `200` |
| `PUT` | `/api/users/{id}` | ADMINISTRADOR | Actualiza nombre/correo; `200` |
| `PATCH` | `/api/users/{id}/status` | ADMINISTRADOR | ACTIVO/INACTIVO; `200` |
| `PUT` | `/api/users/{id}/roles` | ADMINISTRADOR | Reemplaza roles asignados; `200` |
| `GET` | `/api/roles` | ADMINISTRADOR | Lista roles de referencia; `200` |

Los cuerpos de registro y login usan `{ "nombre", "email", "password" }` y `{ "email", "password" }`. El alta administrativa usa `{ "usuario": { "nombre", "email", "password" }, "rolIds": ["uuid"] }`; asignación de roles usa `{ "rolIds": ["uuid"] }`; cambio de estado usa `{ "estado": "INACTIVO" }`. Correos se recortan y pasan a minúsculas. Las contraseñas se guardan con BCrypt (work factor 12), nunca se devuelven.

## Verificación

`mvn test` no necesita PostgreSQL. Las pruebas unitarias y MockMvc no demuestran Flyway, validación JPA, constraints ni persistencia real; esas verificaciones quedan para una fase posterior con una base descartable. La baja no revoca JWT ya emitidos: el vencimiento de 15 minutos limita esa ventana. Parking todavía debe configurar el mismo emisor/audiencia y validar los JWT antes de confiar en `operadorId`.
