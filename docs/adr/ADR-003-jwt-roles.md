# ADR-003: Autenticación JWT sin estado y permisos por rol

- Estado: Aceptada (decisión observada)
- Contexto: Los clientes web llaman a una API remota y necesitan autenticar las peticiones sin sesión HTTP compartida.
- Decisión: La API usa JWT Bearer y Spring Security con sesión stateless; las rutas se autorizan por rol. El frontend guarda token y expiración en `localStorage`, adjunta Bearer y maneja 401. Contraseñas se codifican con BCrypt.
- Consecuencias: Los secretos deben gestionarse fuera del código; el token en almacenamiento accesible por JavaScript aumenta el impacto potencial de XSS. El refresh debe protegerse y verificarse. CORS de producción debe restringirse a orígenes explícitos.
- Evidencia: `SecurityConfig`, `JwtService`, filtro JWT y `frontend/src/services/api.ts`.
- Pendiente: confirmar duración y estrategia de revocación/rotación, protección de XSS y configuración desplegada.
