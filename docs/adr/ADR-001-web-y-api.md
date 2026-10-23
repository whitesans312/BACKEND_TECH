# ADR-001: Aplicación web React y API Spring Boot

- Estado: Aceptada (decisión observada)
- Contexto: El sistema necesita una interfaz operativa para varios roles y una capa central que aplique reglas de negocio y permisos.
- Decisión: Separar el cliente React/TypeScript servido como aplicación web de la API REST implementada con Spring Boot. El frontend accede a la API mediante servicios Axios bajo `/api`.
- Consecuencias: La interfaz y la lógica de negocio pueden evolucionar separadamente; se necesita administrar CORS, URL de API y compatibilidad de contratos. La autorización no debe depender del estado de la interfaz.
- Evidencia: `frontend/src/services/api.ts`, `backend/src/main/java/com/ergpos/app/controllers` y `pom.xml`.
