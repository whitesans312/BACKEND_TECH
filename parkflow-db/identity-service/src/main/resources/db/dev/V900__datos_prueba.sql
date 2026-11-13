-- SOLO LOCAL (db/dev). Contraseñas de prueba: admin = Admin123!  ·  operador = Operador123!
INSERT INTO usuarios (id, nombre, email, password_hash) VALUES
 ('00000000-0000-0000-0000-0000000000a1', 'Admin Prueba',    'admin@parkflow.test',
  '$2b$10$w1rcmJw1SBICoTs9CYst9utktKRJ0ij4XbdLXgiYwOFSO8HGkKPF6'),
 ('00000000-0000-0000-0000-0000000000a2', 'Operador Prueba', 'operador@parkflow.test',
  '$2b$10$yPAQM9r4riuShOZ9eWvzJepimHYuseV0zdLDUvNjY4Hns6i8zNasC');
INSERT INTO usuario_roles (usuario_id, rol_id)
SELECT '00000000-0000-0000-0000-0000000000a1', id FROM roles WHERE nombre = 'ADMINISTRADOR';
INSERT INTO usuario_roles (usuario_id, rol_id)
SELECT '00000000-0000-0000-0000-0000000000a2', id FROM roles WHERE nombre = 'OPERADOR';
