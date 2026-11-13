REVOKE ALL ON SCHEMA public FROM PUBLIC;
GRANT USAGE ON SCHEMA public TO billing_app;
GRANT SELECT, INSERT, UPDATE ON ALL TABLES IN SCHEMA public TO billing_app;
REVOKE DELETE ON ALL TABLES IN SCHEMA public FROM billing_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO billing_app;
ALTER DEFAULT PRIVILEGES FOR ROLE billing_migrator IN SCHEMA public GRANT SELECT, INSERT, UPDATE ON TABLES TO billing_app;
ALTER DEFAULT PRIVILEGES FOR ROLE billing_migrator IN SCHEMA public REVOKE DELETE ON TABLES FROM billing_app;
ALTER DEFAULT PRIVILEGES FOR ROLE billing_migrator IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO billing_app;

INSERT INTO parametros_facturacion(clave, valor, descripcion) VALUES
    ('MINUTOS_GRACIA','15','Minutos iniciales gratuitos'),
    ('HORA_APERTURA','08:00','Hora local de inicio del ciclo diario'),
    ('HORA_CIERRE','22:00','Hora local de cierre operativo'),
    ('ZONA_HORARIA','America/Bogota','Zona horaria operativa'),
    ('UMBRAL_DIARIO_MINUTOS','180','Tiempo facturable estricto para aplicar tarifa diaria'),
    ('RECARGO_NOCTURNO_HABILITADO','false','Pendiente de aprobación comercial; mantener deshabilitado');

INSERT INTO tarifas(tipo_vehiculo, concepto, valor, moneda, vigente_desde, activa) VALUES
    ('CARRO','HORA',6000,'COP','2026-01-01 00:00:00-05',TRUE),
    ('MOTO','HORA',3500,'COP','2026-01-01 00:00:00-05',TRUE),
    ('CARRO','DIA',18000,'COP','2026-01-01 00:00:00-05',TRUE),
    ('MOTO','DIA',10000,'COP','2026-01-01 00:00:00-05',TRUE),
    ('CARRO','RECARGO_NOCTURNO',15000,'COP','2026-01-01 00:00:00-05',TRUE),
    ('MOTO','RECARGO_NOCTURNO',8000,'COP','2026-01-01 00:00:00-05',TRUE);
