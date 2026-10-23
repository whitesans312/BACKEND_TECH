# Queries y Actions

Formato estándar de contrato para describir operaciones de aplicación. Se mapean a la API existente; no constituyen una implementación CQRS.

## Formato

```yaml
id: <dominio>.<verbo>
tipo: Query | Action
objetivo: <resultado para el usuario>
autorizacion: <rol o regla observada>
entrada: <parámetros, filtros o DTO>
salida: <modelo o resultado>
efectos: []                  # solo en Action
errores: [400, 401, 403, 404, 409, 500]
api: <método y ruta>
```

`Query` no debe alterar estado de negocio. `Action` puede persistir cambios o disparar efectos transaccionales. Los contratos siguientes resumen capacidades observadas; los DTO exactos se deben tomar del código cuando se implemente cada integración.

## Auth

| ID | Tipo | Autorización | Entrada → salida | API |
|---|---|---|---|---|
| auth.login | Action | Pública | email, password → token, expiración, usuario | `POST /api/auth/login` |
| auth.me | Query | Autenticado | JWT → usuario actual | `GET /api/auth/me` |
| auth.refresh | Action | Sesión válida | JWT actual → token renovado | `POST /api/auth/refresh` |
| auth.logout | Action | Autenticado | JWT → confirmación de salida | `POST /api/auth/logout` |

## Catálogo e inventario

| ID | Tipo | Autorización | Entrada → salida | API |
|---|---|---|---|---|
| producto.listar | Query | GET autenticado | filtro opcional → productos | `GET /api/productos`, `/activos`, `/buscar`, `/stock-bajo` |
| producto.crear | Action | ADMIN/INVENTARIO | datos de producto → producto creado | `POST /api/productos` |
| producto.actualizar | Action | ADMIN/INVENTARIO | id + cambios → producto actualizado | `PUT /api/productos/{id}` |
| inventario.kardex | Query | ADMIN/INVENTARIO | productoId → movimientos ordenados | `GET /api/kardex/{productoId}` |
| inventario.registrarEntrada | Action | ADMIN/INVENTARIO | producto, cantidad, motivo → movimiento | `POST /api/movimientos-inventario/entrada` |
| inventario.registrarSalida | Action | ADMIN/INVENTARIO | producto, cantidad, motivo → movimiento | `POST /api/movimientos-inventario/salida` |
| compra.crear | Action | ADMIN/INVENTARIO | proveedor + ítems → compra | `POST /api/compras` |
| compra.confirmar | Action | ADMIN/INVENTARIO | compraId → compra confirmada; genera entradas | `PATCH /api/compras/{id}/confirmar` |

## Ventas y facturación

| ID | Tipo | Autorización | Entrada → salida | API |
|---|---|---|---|---|
| venta.listar | Query | ADMIN/VENDEDOR | filtros admitidos → ventas | `GET /api/ventas`, `/recientes`, `/resumen` |
| venta.obtener | Query | ADMIN/VENDEDOR | ventaId → detalle | `GET /api/ventas/{id}` |
| venta.registrar | Action | ADMIN/VENDEDOR | cliente opcional + vendedor + ítems → venta | `POST /api/ventas` |
| venta.anular | Action | ADMIN/VENDEDOR | ventaId → estado actualizado/anulación | `DELETE /api/ventas/{id}` |
| facturaVenta.generar | Action | ADMIN/VENDEDOR | ventaId → factura | `POST /api/facturas-venta/generar/{ventaId}` |
| facturaVenta.consultar | Query | ADMIN/VENDEDOR | ventaId o facturaId → factura | `GET /api/facturas-venta/...` |

## Órdenes, clientes y garantías

| ID | Tipo | Autorización | Entrada → salida | API |
|---|---|---|---|---|
| orden.listar | Query | ADMIN/TECNICO | estado/técnico opcional → órdenes | `GET /api/entregas`, `/estado/{estado}`, `/tecnico/{tecnicoId}` |
| orden.crear | Action | ADMIN | cliente, técnico, tipo, ítems → orden | `POST /api/entregas` |
| orden.actualizar | Action | ADMIN | ordenId + cambios → orden | `PUT /api/entregas/{id}` |
| orden.cambiarEstado | Action | ADMIN/TECNICO, según regla | ordenId + estado/notas → orden | `PATCH /api/entregas/{id}/estado` |
| orden.confirmar | Action | ADMIN | ordenId → finalización y actualización de inventario | `POST /api/entregas/{id}/confirmar` |
| orden.registrarPago | Action | ADMIN | ordenId + monto/notas → pago y saldo actualizado | `POST /api/entregas/{id}/pagos` |
| cliente.buscar | Query | ADMIN/VENDEDOR y roles autorizados | término → coincidencias | `GET /api/clientes/buscar` |
| garantia.crear | Action | ADMIN/VENDEDOR/TECNICO | venta u orden + ítems + motivo → devolución/garantía | `POST /api/devoluciones-garantias` |

## Caja y supervisión

| ID | Tipo | Autorización | Entrada → salida | API |
|---|---|---|---|---|
| caja.actual | Query | Autenticado | usuario/sesión → caja actual | `GET /api/cajas/actual` |
| caja.abrir | Action | Según permisos de la ruta | monto inicial + notas → caja abierta | `POST /api/cajas/abrir` |
| caja.cerrar | Action | Según permisos de la ruta | cajaId + monto final → cierre | `PATCH /api/cajas/{id}/cerrar` |
| caja.registrarMovimiento | Action | Según permisos de la ruta | tipo, concepto, monto → movimiento | `POST /api/cajas/{cajaId}/movimientos` |
| dashboard.resumen | Query | Autenticado | sin entrada → KPIs del dashboard | `GET /api/dashboard/resumen` |
| analitica.flujoCaja | Query | Rol permitido por API | rango/parámetros → flujo y proyección | `GET /api/analitica/flujo-caja` |
| reporte.exportarVentas | Query | ADMIN | filtros de reporte → archivo Excel/PDF | `GET /api/reportes/exportar/ventas/...` |
| auditoria.listar | Query | ADMIN | filtros → eventos auditados | `GET /api/auditoria` |

## Tratamiento de errores esperado

- `400`: formato inválido o regla de negocio incumplida.
- `401`: credenciales/token ausentes, expirados o inválidos.
- `403`: rol sin permiso para el recurso.
- `404`: identificador inexistente.
- `409`: conflicto de estado, duplicado o falta de stock, si el handler lo representa como conflicto.
- `500`: fallo no controlado; el cliente debe mostrar un mensaje seguro y no detalles internos.

Los códigos exactos pueden variar por controlador y `GlobalExceptionHandler`; confirmar el mapeo antes de tratar este documento como contrato OpenAPI.
