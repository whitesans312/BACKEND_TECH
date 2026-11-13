\set ON_ERROR_STOP on
\set VERBOSITY terse
BEGIN;
\i tests/_helpers.sql
SELECT pg_temp.assert_eq((SELECT string_agg(nombre, ',' ORDER BY nombre) FROM roles), 'ADMINISTRADOR,OPERADOR', 'roles de referencia');
SELECT pg_temp.expect($$INSERT INTO usuarios (nombre,email,password_hash) VALUES ('X','ADMIN@PARKFLOW.TEST','hash')$$, '23505', 'RN-I1 correo único sin distinguir mayúsculas', 'uq_usuarios_email');
SELECT pg_temp.expect($$INSERT INTO usuarios (nombre,email,password_hash) VALUES ('X','x@parkflow.test','   ')$$, '23514', 'password_hash vacío', 'ck_usuarios_hash');
SELECT pg_temp.expect($$INSERT INTO usuarios (nombre,email,password_hash) VALUES ('X','x@parkflow.test',NULL)$$, '23502', 'password_hash NULL');
SELECT pg_temp.expect($$INSERT INTO usuarios (nombre,email,password_hash,estado) VALUES ('X','y@parkflow.test','h','BLOQUEADO')$$, '23514', 'estado inválido', 'ck_usuarios_estado');
SELECT pg_temp.expect($$INSERT INTO usuario_roles (usuario_id,rol_id) SELECT '00000000-0000-0000-0000-0000000000a1', id FROM roles WHERE nombre='ADMINISTRADOR'$$, '23505', 'RN-I2 rol repetido para el usuario', 'pk_usuario_roles');
SELECT pg_temp.expect_ok($$INSERT INTO usuario_roles (usuario_id,rol_id) SELECT '00000000-0000-0000-0000-0000000000a1', id FROM roles WHERE nombre='OPERADOR'$$, 'un usuario con dos roles');
SELECT pg_temp.expect($$DELETE FROM usuarios WHERE email='admin@parkflow.test'$$, '23503', 'FK RESTRICT: borrar usuario con roles');
SELECT pg_temp.expect($$SET LOCAL ROLE identity_app; DELETE FROM usuarios$$, '42501', 'identity_app sin DELETE en usuarios');
RESET ROLE;
SELECT pg_temp.expect_ok($$SET LOCAL ROLE identity_app; DELETE FROM usuario_roles WHERE rol_id IN (SELECT id FROM roles WHERE nombre='OPERADOR') AND usuario_id='00000000-0000-0000-0000-0000000000a1'$$, 'identity_app puede quitar un rol (excepción a confirmar)');
RESET ROLE;
ROLLBACK;
\echo 'IDENTITY: todas las pruebas pasaron'
