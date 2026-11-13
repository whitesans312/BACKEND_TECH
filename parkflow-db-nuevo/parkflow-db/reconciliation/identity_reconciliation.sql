-- Consultas de solo lectura
SELECT u.id, u.email FROM usuarios u LEFT JOIN usuario_roles ur ON ur.usuario_id=u.id WHERE u.estado='ACTIVO' GROUP BY u.id,u.email HAVING count(ur.rol_id)=0;
SELECT id, jti, expira_en FROM autorizaciones_excepcionales WHERE estado='EMITIDA' AND expira_en < now();
