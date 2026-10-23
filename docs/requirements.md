# Descripción y requisitos del proyecto

## Descripción

ERG-POS es una aplicación web para centralizar la operación de un negocio de repuestos de sillas ergonómicas y servicios técnicos. Desde una interfaz React, los usuarios gestionan clientes, catálogo, compras, inventario, ventas de mostrador, órdenes de servicio, entregas, pagos, caja, devoluciones y garantías. El backend Spring Boot expone una API REST protegida; PostgreSQL almacena los datos y aplica reglas como actualización transaccional del stock. El sistema ofrece tableros, analítica, exportaciones y un asistente conversacional para administración.

## Actores

| Actor | Responsabilidad observada |
|---|---|
| Administrador | Configuración, usuarios, reportes, auditoría, órdenes, operaciones comerciales y administración general. |
| Vendedor | Ventas, clientes y facturas de venta. |
| Técnico | Consulta y actualización de órdenes según permisos de la API. |
| Inventario | Productos, proveedores, compras y movimientos de inventario. |

Los roles y permisos efectivos se definen en `SecurityConfig`; la matriz concreta debe revisarse allí cuando cambie la autorización.

## Requisitos funcionales por épica

### Épica F1 — Identidad y acceso

- **F1.1 (Implementado):** iniciar sesión con credenciales y recibir token JWT.
- **F1.2 (Implementado):** consultar identidad de sesión, renovar token y cerrar sesión desde el cliente.
- **F1.3 (Implementado):** administrar usuarios y roles, activar/desactivar cuentas y cambiar contraseña conforme a permisos.
- **F1.4 (Implementado):** restringir recursos mediante roles ADMIN, VENDEDOR, TECNICO e INVENTARIO.
- **F1.5 (Pendiente de validar):** recuperación de contraseña, MFA, revocación inmediata de tokens y política de bloqueo ante intentos fallidos.

### Épica F2 — Catálogos y relaciones comerciales

- **F2.1 (Implementado):** crear, consultar y actualizar clientes, proveedores, categorías y productos.
- **F2.2 (Implementado):** buscar clientes/proveedores/productos y filtrar registros activos.
- **F2.3 (Implementado):** conservar datos de cliente como snapshot en ventas y órdenes para preservar el historial.
- **F2.4 (Implementado):** mantener configuración del negocio, incluidos exportación/importación y plantillas disponibles en la API.

### Épica F3 — Inventario y abastecimiento

- **F3.1 (Implementado):** consultar existencias, alertas de stock bajo y kardex por producto.
- **F3.2 (Implementado):** registrar entradas y salidas manuales con motivo y actor.
- **F3.3 (Implementado):** crear compras a proveedores y confirmarlas/cancelarlas según estado.
- **F3.4 (Implementado):** reflejar altas/bajas de stock mediante movimientos de inventario y trigger de base de datos.
- **F3.5 (Pendiente de validar):** políticas de conteo físico, ajustes con doble aprobación y reservas de stock concurrentes.

### Épica F4 — Ventas y facturación

- **F4.1 (Implementado):** registrar ventas POS con productos, cantidades, cliente y vendedor.
- **F4.2 (Implementado):** consultar ventas, recientes y resumen; anular según reglas del backend.
- **F4.3 (Implementado):** emitir/consultar factura de venta y exportar reportes PDF/Excel.
- **F4.4 (Pendiente de validar):** integración con facturación electrónica fiscal, impuestos y numeración legal del país. La numeración interna FV no demuestra dicha integración.

### Épica F5 — Órdenes de servicio y pagos

- **F5.1 (Implementado):** crear y editar órdenes/entregas, asignar técnico y consultar por estado o técnico.
- **F5.2 (Implementado):** transicionar estado de la orden; confirmar el servicio y registrar anticipos/pagos parciales.
- **F5.3 (Implementado):** consultar pendientes de pago/confirmación y resúmenes de órdenes.
- **F5.4 (Implementado):** registrar devoluciones y garantías asociadas a ventas u órdenes.
- **F5.5 (Pendiente de validar):** notificaciones al cliente, comprobante de aceptación del trabajo y políticas de garantía por tipo de producto.

### Épica F6 — Caja y finanzas operativas

- **F6.1 (Implementado):** abrir/cerrar caja y consultar caja actual, cajas abiertas y resumen.
- **F6.2 (Implementado):** registrar ingresos, egresos, pagos y movimientos de caja.
- **F6.3 (Implementado):** consultar flujo histórico y proyección, además de exportar información financiera.
- **F6.4 (Pendiente de validar):** conciliación bancaria, cierres auditables por turno y reglas de diferencias de efectivo.

### Épica F7 — Supervisión y analítica

- **F7.1 (Implementado):** mostrar dashboard, analítica de técnicos, inventario, rentabilidad por categoría, ventas por vendedor y flujo de caja.
- **F7.2 (Implementado):** consultar bitácora de auditoría y exportar reportes disponibles.
- **F7.3 (Implementado):** consultar asistente conversacional desde el frontend; el acceso de API requiere ADMIN.
- **F7.4 (Pendiente de validar):** exactitud de métricas predictivas, trazabilidad de fuentes de IA y límites de uso/costo del proveedor externo.

## Requisitos no funcionales

| ID | Requisito | Estado/evidencia |
|---|---|---|
| NF-01 Seguridad | La API debe autenticar peticiones protegidas con JWT, aplicar roles y almacenar contraseñas con BCrypt. | Parcialmente implementado; verificar bootstrap y configuración de producción. |
| NF-02 Integridad | Las operaciones de inventario deben ser atómicas y no permitir existencias negativas. | Implementado en el diseño de BD con trigger y excepciones; validar que el SQL desplegado coincide. |
| NF-03 Auditoría | Registrar operaciones relevantes con actor, fecha y metadatos, preservando inmutabilidad del log. | Esquema/controlador presentes; cobertura de eventos debe validarse. |
| NF-04 Usabilidad | Interfaz web en español con navegación por módulos, búsqueda, indicadores y formatos monetarios/locales. | Implementado parcialmente en páginas y utilidades frontend. |
| NF-05 Interoperabilidad | Frontend y backend se comunican por JSON/HTTP; CORS se configura por patrones de origen. | Implementado. En producción se deben limitar los orígenes. |
| NF-06 Mantenibilidad | Separar presentación, servicios API, controladores, servicios de negocio, repositorios y entidades. | Implementado en la estructura actual. |
| NF-07 Rendimiento | Las lecturas operativas deben responder en tiempos aceptables bajo la carga acordada. | Objetivo por definir; el repositorio no contiene SLO ni pruebas de carga. |
| NF-08 Disponibilidad/recuperación | Definir disponibilidad, backups, RPO/RTO y restauración de PostgreSQL. | Pendiente de operación/infraestructura. |
| NF-09 Compatibilidad | Soportar navegadores modernos y despliegue del frontend estático con API remota. | Diseño compatible; matriz de navegadores por acordar. |
| NF-10 Privacidad | Minimizar exposición de datos personales, credenciales y respuestas de terceros; mantener secretos fuera del repositorio. | Requiere revisión de configuración y despliegue. |

## Requisitos técnicos

| ID | Requisito técnico |
|---|---|
| RT-01 | Frontend: React 19 + TypeScript + Vite; rutas con React Router; Axios centraliza URL, JWT y manejo de 401. |
| RT-02 | Backend: Java 21 y Spring Boot 3.5 con Spring Web, Security, Validation y Data JPA. |
| RT-03 | Persistencia PostgreSQL; el esquema y funciones se mantienen en `BD_ERG_POS_COMPLETA.sql` y se describen en `DATABASE_SCHEMA.md`. |
| RT-04 | API versionada por convención bajo `/api`; DTO/JSON como interfaz de integración. |
| RT-05 | JWT HS256 sin sesión de servidor; secretos y credenciales se inyectan mediante variables de entorno. |
| RT-06 | Archivos de reportes: Excel con Apache POI y PDF con OpenPDF. |
| RT-07 | Integración de IA vía backend (`GroqService`), con credencial gestionada como secreto. |
| RT-08 | Despliegue configurable por `PORT`, URL/usuario/contraseña de BD, clave JWT, expiración y orígenes CORS. |

## Criterios transversales de aceptación

1. Toda lectura o comando requiere sesión salvo login/bootstrap y rutas explícitamente públicas.
2. Un comando inválido devuelve error comprensible y no deja cambios parciales en venta, pago, compra o stock.
3. Cada operación de inventario queda representada por un movimiento; el stock no se corrige directamente desde la aplicación.
4. Las acciones permitidas dependen del rol en servidor, aunque la interfaz oculte controles no autorizados.
5. Los reportes identifican el período y usan las reglas de negocio documentadas para interpretar estados y saldos.
