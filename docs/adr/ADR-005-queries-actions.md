# ADR-005: Contratos lógicos de lectura y comando

- Estado: Propuesta de documentación
- Contexto: El frontend invoca servicios por módulo y la API expone recursos REST; conviene describir de modo uniforme qué lecturas están disponibles y qué comandos cambian estado.
- Decisión propuesta: Documentar lecturas como `Query` y mutaciones como `Action`, cada una con propósito, rol mínimo, parámetros, respuesta, efectos y errores. Mapear cada contrato a las rutas REST existentes.
- Consecuencias: Mejora la revisión funcional y prepara la evolución de la API, pero no implica adoptar CQRS, colas, mediadores ni separar almacenes de lectura/escritura.
- Evidencia: Servicios en `frontend/src/services` y controladores en `backend/src/main/java/com/ergpos/app/controllers`.
- Revisión: Validar el formato con el equipo antes de tomarlo como contrato formal de producto.
