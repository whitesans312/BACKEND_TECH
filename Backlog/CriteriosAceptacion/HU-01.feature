# language: es
@HU-01 @E1
Característica: Inicio de sesión
  Como operador, quiero iniciar sesión para usar el sistema de forma segura.

  Escenario: Credenciales válidas
    Dado que existe un usuario activo con rol OPERADOR
    Cuando envía su correo y contraseña correctos a POST /api/auth/login
    Entonces recibe un JWT firmado con RS256
    Y el token identifica al usuario y su rol

  Escenario: Credenciales inválidas
    Dado que existe un usuario activo
    Cuando envía una contraseña incorrecta
    Entonces el acceso es rechazado
    Y no se emite ningún token
