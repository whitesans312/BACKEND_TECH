# Auditoría de seguridad — Identity Service

**Alcance:** revisión estática de Identity y sus pruebas automatizadas, sin PostgreSQL ni despliegue. Se corrigieron los problemas demostrados dentro del servicio. La integración criptográfica con la base real y la configuración de red quedan pendientes para la fase de integración.

## Resultado

La configuración valida JWT RS256 con firma, `issuer`, `audience` y expiración. También se rechazan tokens fuera de su ventana temporal (`nbf`, cuando está presente) por el decoder estándar de Spring Security. El `sub` representa el UUID del usuario y los roles se expresan en `roles`.

La autorización de Identity ya no depende solo de los roles copiados al token: un convertidor de autenticación vuelve a cargar el usuario y sus roles actuales en cada solicitud autenticada. Un usuario inexistente o INACTIVO se rechaza como token inválido. `GET /api/auth/me` aplica la misma regla. Esto reduce la ventana de uso dentro de Identity después de desactivar una cuenta o cambiar sus roles.

El registro público asigna exclusivamente `OPERADOR`; el solicitante no puede elegir roles. La asignación de `ADMINISTRADOR` exige autorización administrativa. Las contraseñas se procesan con BCrypt (work factor 12), los errores de login no revelan si existe la cuenta y la clave tiene un límite de bytes compatible con BCrypt.

JWKS publica la clave pública y su `kid`, nunca la privada. La configuración restringe el algoritmo a RS256. Hay prueba negativa para RS384, firma incorrecta, issuer incorrecto, audiencia incorrecta y expiración.

## Cambios realizados

- Se añadió `IdentityJwtAuthenticationConverter` para comprobar en cada solicitud el estado actual del usuario y cargar sus roles vigentes.
- Se conectó ese convertidor a la configuración del resource server.
- Se hizo que la consulta del usuario actual rechace usuarios inactivos.
- Se ampliaron las pruebas de autorización y JWT para cubrir usuario inactivo, roles vigentes, JWKS público y algoritmo no permitido.

## Persistencia y límites de la auditoría

La revisión de entidades y migraciones encontró correspondencia en nombres de tablas y columnas para usuarios, roles y la tabla de relación `usuario_roles`. La inserción de la relación aprovecha el valor predeterminado de `created_at` en la base. No se cambiaron migraciones.

`mvn clean test` en `services/identity-service` terminó correctamente: **23 pruebas, 0 fallos, 0 errores**. Estas pruebas no verifican `ddl-auto=validate`, constraints, grants, triggers, Flyway ni persistencia contra PostgreSQL; eso requiere una base descartable en la fase de integración.

## Riesgos y trabajo pendiente

- Los otros servicios deben validar JWT por sí mismos. Un JWT emitido antes de desactivar a un usuario aún puede ser aceptado por un consumidor que solo compruebe firma y claims hasta que expire (máximo 15 minutos en la configuración actual). No hay revocación distribuida ni introspección.
- La rotación configurada opera con una sola clave activa. Para rotación sin fallos se debe soportar publicación simultánea de claves públicas durante el solapamiento, o coordinar expiración y cachés JWKS de todos los consumidores antes de cambiar la clave.
- Confirmar comportamiento de Hibernate, constraints, grants y migraciones con PostgreSQL antes de declarar la persistencia validada.
- Confirmar que cada consumidor aplica allowlist RS256, issuer, audience, expiración y su propia autorización de roles; no confiar en cabeceras reenviadas por Gateway.

## Archivos revisados/modificados

La implementación de la auditoría queda en `services/identity-service`, incluyendo `IdentityJwtAuthenticationConverter`, `SecurityConfiguration`, `IdentityService` y sus pruebas. No se modificaron servicios ajenos, frontend ni SQL.
