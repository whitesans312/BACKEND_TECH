# Documentación de ERG-POS

Documentación funcional y técnica del sistema POS para operación de repuestos y servicios técnicos. Se contrastaron el frontend React/TypeScript, la API Spring Boot y el esquema PostgreSQL disponibles en el repositorio.

## Documentos

- [Descripción y requisitos](requirements.md): propósito, alcance, actores, requisitos funcionales, no funcionales y técnicos organizados como épicas.
- [ADRs](adr/README.md): decisiones de arquitectura observadas y decisiones recomendadas que requieren validación del equipo.
- [Diagramas de secuencia](sequences/README.md): escenarios en PlantUML.
- [Queries y Actions](queries-actions.md): contratos de lectura y comandos en formato estándar del dominio.

## Convenciones y alcance

- Los requisitos etiquetados **Implementado** se infieren de pantallas, servicios, controladores y esquema presentes; describen el comportamiento visible, no una certificación de producción.
- Los elementos **Pendiente de validar** son expectativas o cualidades que el repositorio no garantiza por sí solo.
- `Query` representa una lectura sin efecto de negocio. `Action` representa un comando que puede cambiar estado. Son contratos lógicos sobre la API actual, no una afirmación de que el código esté organizado como CQRS o use un bus de mensajes.
- Los detalles sensibles (credenciales, secretos, datos reales) no deben incluirse en ejemplos de documentación.

## Tecnologías observadas

Frontend: React 19, TypeScript 5.9, Vite 7, React Router y Axios. Backend: Java 21, Spring Boot 3.5, Spring MVC, Spring Security, Spring Data JPA/Hibernate, Bean Validation y PostgreSQL. La configuración apunta a Supabase como proveedor PostgreSQL. Reportes usan Apache POI y OpenPDF; el asistente se integra con Groq mediante servicio backend.

## Renderizar PlantUML

Los archivos `.puml` son fuentes PlantUML. Pueden abrirse con una extensión PlantUML o renderizarse con PlantUML CLI; este repositorio no fija aún una herramienta de renderizado.
