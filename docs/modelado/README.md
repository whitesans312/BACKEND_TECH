# Modelado técnico ParkFlow

Los diez documentos siguientes describen el estado encontrado en código, migraciones y configuración. El [consolidado](MODELADO_TECNICO_COMPLETO_PARKFLOW.md) reúne todos los módulos.

1. [Modelo de datos de Parking](01_MODELO_DB_PARKING.md)
2. [Modelo de datos de Identity](02_MODELO_DB_IDENTITY.md)
3. [Modelo de datos de Billing](03_MODELO_DB_BILLING.md)
4. [Diccionario completo de datos](04_DICCIONARIO_DATOS_COMPLETO.md)
5. [Arquitectura de microservicios](05_MODELO_ARQUITECTURA_MICROSERVICIOS.md)
6. [Componentes internos](06_COMPONENTES_INTERNOS.md)
7. [Comunicación y contratos REST](07_COMUNICACION_Y_CONTRATOS_REST.md)
8. [Fuentes de diagramas](08_DIAGRAMAS_PLANTUML_MERMAID.md)
9. [Despliegue local](09_DESPLIEGUE.md)
10. [Diferencias entre diseño e implementación](10_DIFERENCIAS_ENTRE_DISENO_E_IMPLEMENTACION.md)

## Conteos verificados

- 3 microservicios backend.
- 33 tablas en las migraciones: 14 Parking, 7 Identity y 12 Billing.
- 37 restricciones FOREIGN KEY locales; no hay FK cross-database.
- 261 campos extraídos al diccionario de datos.
- 50 mappings de endpoints anotados en los controladores inspeccionados.
- 2 secuencias PostgreSQL: `seq_ticket` en Parking y `seq_comprobante` en Billing.

## Fuentes

Migraciones activas bajo `../../parkflow-db-definitivo/`, código Java bajo `../../services/`, `../../docker-compose.parkflow.yml`, los POM de los servicios y `../../PARKFLOW_IMPLEMENTATION_ROADMAP.md`. El paquete `parkflow-db-nuevo/` se trata como histórico.
