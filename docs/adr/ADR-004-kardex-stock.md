# ADR-004: El stock cambia a través del kardex

- Estado: Aceptada (decisión observada)
- Contexto: Ventas, órdenes, compras y ajustes concurrentes deben conservar un historial explicable y evitar desajustes en existencias.
- Decisión: Registrar cada entrada o salida en `movimientos_inventario`; la base de datos actualiza el stock mediante trigger y rechaza movimientos no válidos. La aplicación no debe editar directamente `productos.stock`.
- Consecuencias: El movimiento es el registro contable de inventario y forma parte de la transacción de negocio. Cambiar trigger y backend requiere despliegue coordinado.
- Evidencia: `DATABASE_SCHEMA.md`, script SQL y `MovimientoInventarioService`, `VentaService`, `CompraService`.
