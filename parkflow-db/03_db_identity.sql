-- ParkFlow · db_identity · V1 · esquema inicial y roles de referencia
CREATE TABLE usuarios (
    id             uuid         NOT NULL DEFAULT gen_random_uuid(),
    nombre         varchar(120) NOT NULL,
    email          varchar(120) NOT NULL,
    password_hash  varchar(255) NOT NULL,
    estado         varchar(20)  NOT NULL DEFAULT 'ACTIVO',
    created_at     timestamptz  NOT NULL DEFAULT now(),
    updated_at     timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT pk_usuarios PRIMARY KEY (id),
    CONSTRAINT ck_usuarios_nombre CHECK (btrim(nombre) <> ''),
    CONSTRAINT ck_usuarios_email  CHECK (btrim(email) <> '' AND position('@' IN email) > 1),
    CONSTRAINT ck_usuarios_hash   CHECK (btrim(password_hash) <> ''),
    CONSTRAINT ck_usuarios_estado CHECK (estado IN ('ACTIVO', 'INACTIVO'))
);
-- RN-I1: correo único sin distinguir mayúsculas (sin citext)
CREATE UNIQUE INDEX uq_usuarios_email ON usuarios (lower(email));

CREATE TABLE roles (
    id           uuid        NOT NULL DEFAULT gen_random_uuid(),
    nombre       varchar(30) NOT NULL,
    descripcion  varchar(120),
    created_at   timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_roles PRIMARY KEY (id),
    CONSTRAINT uq_roles_nombre UNIQUE (nombre),
    CONSTRAINT ck_roles_nombre CHECK (btrim(nombre) <> '')
);

CREATE TABLE usuario_roles (
    usuario_id  uuid        NOT NULL,
    rol_id      uuid        NOT NULL,
    created_at  timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_usuario_roles PRIMARY KEY (usuario_id, rol_id),
    CONSTRAINT fk_ur_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE RESTRICT,
    CONSTRAINT fk_ur_rol     FOREIGN KEY (rol_id)     REFERENCES roles (id)    ON DELETE RESTRICT
);
CREATE INDEX idx_ur_rol ON usuario_roles (rol_id);

-- Datos de referencia (todos los entornos)
INSERT INTO roles (nombre, descripcion) VALUES
    ('ADMINISTRADOR', 'Administra pisos, espacios, tarifas, usuarios y anulaciones'),
    ('OPERADOR',      'Registra entradas, liquida, cobra y cierra salidas');
-- Sin DELETE en tablas de negocio (RN-I3). EXCEPCIÓN A CONFIRMAR: usuario_roles permite DELETE
-- porque, de lo contrario, no se podría quitar un rol a un usuario (ver informe de pruebas).
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'identity_app') THEN
        GRANT USAGE ON SCHEMA public TO identity_app;
        GRANT SELECT, INSERT, UPDATE ON ALL TABLES IN SCHEMA public TO identity_app;
        GRANT DELETE ON usuario_roles TO identity_app;
    END IF;
END $$;
