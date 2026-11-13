# language: es
@HU-02 @E1
Característica: Gestión de usuarios y roles
  Como administrador, quiero gestionar usuarios y roles.

  Escenario: Crear usuario con roles
    Dado que el administrador está autenticado
    Cuando crea un usuario con nombre, correo, contraseña y rolIds en POST /api/users
    Entonces el usuario queda creado con los roles indicados

  Escenario: Un operador no administra usuarios
    Dado que un operador está autenticado
    Cuando intenta crear o modificar un usuario
    Entonces el sistema rechaza la operación por falta de permisos
