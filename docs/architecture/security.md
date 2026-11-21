# Seguridad Multi-Cadena

La seguridad de `loyalty-engine` implementa Spring Security 6 con tres cadenas de filtros independientes y ordenadas según el canal de entrada:

```text
[Incoming Request]
       │
       ├── /api/v1/events, /api/v1/coupons/** ──> Order(1): integrationSecurity (Stateless, ApiKeyFilter, ROLE_INTEGRATION_SERVICE)
       │
       ├── /api/v1/**                        ──> Order(2): applicationSecurity (REST API, JsonAuthenticationFilter, 401 EntryPoint)
       │
       └── /**                               ──> Order(3): portalSecurity (Backoffice Web, formLogin, Session)
```

---

## Cadena 1 (`@Order(1)` — Ingesta POS & Cupones M2M)

- **Patrón de URL**: `/api/v1/events`, `/api/v1/coupons/**`
- **Mecanismo de Autenticación**: `ApiKeyFilter` personalizado que evalúa la cabecera HTTP `X-API-KEY`.
- **Política de Sesión**: `SessionCreationPolicy.STATELESS` (sin sesiones ni cookies).
- **Protección Anti-Fuzzing**: Mitiga ataques de fuerza bruta en cupones limitando la tasa de consultas por API Key/IP y acotando la búsqueda estrictamente al `tenant_id` autenticado.
- **CSRF**: Deshabilitado (adecuado para comunicaciones M2M autenticadas por clave única).
- **Rol Asignado**: `Roles.ROLE_INTEGRATION_SERVICE`.
- **Aislamiento Multi-Tenant**: El filtro valida el hash de la API Key, identifica la `PosIntegration` asociada y enlaza el `tenantId` en el `SecurityContext` para que los servicios downstream operen con contexto verificado.

---

## Cadena 2 (`@Order(2)` — REST API de Aplicación)

- **Patrón de URL**: `/api/v1/**`
- **Mecanismo de Autenticación**: `JsonAuthenticationFilter` que procesa credenciales enviadas como JSON `POST` a `/api/auth/login`.
- **Punto de Entrada No Autenticado**: `HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)`.
  > **Crítico**: Los endpoints REST deben responder con HTTP 401 ante accesos no autorizados. Bajo ninguna circunstancia deben emitir una redirección HTTP 302 a un formulario de inicio de sesión HTML.
- **Cierre de Sesión**: `POST /api/auth/logout` respondiendo con `HTTP 204 No Content`.

---

## Cadena 3 (`@Order(3)` — Portal Web / Backoffice)

- **Patrón de URL**: `/**` (rutas restantes)
- **Mecanismo de Autenticación**: `formLogin` estándar redirigiendo a `/dashboard` tras autenticación exitosa.
- **Uso**: Panel administrativo para visualización de métricas de fidelización y configuración de tenants.

---

## Reglas de Desacoplamiento de Transporte

Los servicios de negocio marcados con `@Service` son completamente independientes del protocolo de comunicación:
- **PROHIBIDO** inyectar `HttpServletRequest`, `HttpServletResponse` o `HttpSession` dentro de una clase de servicio.
- Si un servicio necesita la identidad del comercio autenticado, la obtiene desde un proveedor de contexto de seguridad (`TenantSecurityContext`) o como parámetro tipado en su método.
