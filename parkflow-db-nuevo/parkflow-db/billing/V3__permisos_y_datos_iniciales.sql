-- Ejecutar conectado a db_billing como billing_migrator.
GRANT CONNECT ON DATABASE db_billing TO billing_app;
GRANT USAGE ON SCHEMA public TO billing_app;
GRANT SELECT, INSERT, UPDATE ON ALL TABLES IN SCHEMA public TO billing_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO billing_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE ON TABLES TO billing_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO billing_app;
REVOKE DELETE ON ALL TABLES IN SCHEMA public FROM billing_app;

-- Parámetros operativos confirmados por el negocio. Java Billing interpreta estos valores.
INSERT INTO parametros_facturacion(clave, valor, descripcion) VALUES
 ('MINUTOS_GRACIA','15','Minutos iniciales sin cobro'),
 ('HORA_APERTURA','08:00','Hora local de apertura; zona America/Bogota'),
 ('HORA_CIERRE','22:00','Hora local de cierre; zona America/Bogota'),
 ('ZONA_HORARIA','America/Bogota','Zona horaria del negocio');

-- Tarifas iniciales confirmadas. Vigencia inicial abierta desde la fecha de despliegue.
-- Se cargan conceptos separados para que Java aplique la regla temporal acordada.
INSERT INTO tarifas(tipo_vehiculo, concepto, valor, vigente_desde, activa) VALUES
 ('CARRO','HORA',6000,now(),TRUE),
 ('MOTO','HORA',3500,now(),TRUE),
 ('CARRO','DIA',18000,now(),TRUE),
 ('MOTO','DIA',10000,now(),TRUE),
 ('CARRO','RECARGO_NOCTURNO',15000,now(),TRUE),
 ('MOTO','RECARGO_NOCTURNO',8000,now(),TRUE);
