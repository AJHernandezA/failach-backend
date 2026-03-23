# F011 — Authentication & Authorization (Backend)

## Propósito

Implementar validación de JWT de Cognito, middleware de autenticación, extracción de roles y protección de endpoints administrativos.

## Endpoints

| Método | Ruta                   | Descripción                 | Auth |
| ------ | ---------------------- | --------------------------- | ---- |
| `POST` | `/api/v1/auth/login`   | Proxy de login a Cognito    | No   |
| `POST` | `/api/v1/auth/refresh` | Refresh token               | No   |
| `GET`  | `/api/v1/auth/me`      | Obtener usuario autenticado | Sí   |

## Infrastructure Layer

### AuthFilter (middleware/)

- Se ejecuta SOLO en rutas marcadas como protegidas.
- Lee header `Authorization: Bearer <token>`.
- Si no existe → `401 Unauthorized`.
- Valida JWT contra las public keys de Cognito (JWKS endpoint).
- Extrae claims: `sub`, `email`, `custom:role`, `custom:tenantId`.
- Crea `AuthenticatedUser` y lo setea en `ctx.attribute("user")`.
- Validación de tenant: si el rol es MERCHANT, verifica que `custom:tenantId` coincide con `X-Tenant-Id`.

### JWT Validation

- Descargar y cachear JWKS (JSON Web Key Set) de Cognito.
- URL: `https://cognito-idp.{region}.amazonaws.com/{userPoolId}/.well-known/jwks.json`.
- Verificar: firma, expiración, issuer, audience.
- Cachear keys por 1 hora (rotación de keys de Cognito).

### AuthenticatedUser (record)

```java
public record AuthenticatedUser(
    String sub,
    String email,
    UserRole role,
    String tenantId
) {}
```

### Role Enforcement

- SUPER_ADMIN: acceso a todos los tenants y endpoints.
- MERCHANT: acceso solo a endpoints de SU tenant.
- Para cada endpoint protegido, definir los roles permitidos.
- Si rol insuficiente → `403 Forbidden`.

### Endpoints Protegidos (resumen)

| Endpoint                            | Roles Permitidos                       |
| ----------------------------------- | -------------------------------------- |
| `PUT /tenants/:id/config`           | SUPER_ADMIN, MERCHANT (solo su tenant) |
| `POST /products`                    | SUPER_ADMIN, MERCHANT                  |
| `PUT /products/:id`                 | SUPER_ADMIN, MERCHANT                  |
| `DELETE /products/:id`              | SUPER_ADMIN, MERCHANT                  |
| `POST /categories`                  | SUPER_ADMIN, MERCHANT                  |
| `PUT /orders/:code/status`          | SUPER_ADMIN, MERCHANT                  |
| `PUT /orders/:code/confirm-payment` | SUPER_ADMIN, MERCHANT                  |
| `PUT /orders/:code/cancel`          | SUPER_ADMIN, MERCHANT                  |
| `GET /orders` (lista)               | SUPER_ADMIN, MERCHANT                  |
| `GET /auth/me`                      | Cualquier autenticado                  |

### Endpoints Públicos (NO requieren auth)

- `GET /health`
- `GET /tenants/:id/config`
- `GET /products`, `GET /products/:id`
- `GET /categories`
- `GET/POST/PUT/DELETE /cart/*`
- `POST /orders` (crear orden)
- `GET /orders/:code` (tracking)
- `POST /payments/wompi/init`
- `POST /webhooks/wompi`
- `GET /storefront`
- `GET /sitemap-data`
- `GET /legal/:type`

## Criterios de Aceptación

- [x] JWT válido permite acceso a endpoints protegidos. _(MockAuth en dev, AuthFilter listo para Cognito)_
- [x] JWT expirado retorna `401`. _(AuthFilter implementado)_
- [x] JWT con firma inválida retorna `401`. _(AuthFilter implementado)_
- [x] Sin header Authorization retorna `401`.
- [x] MERCHANT de tenant A accediendo a tenant B retorna `403`.
- [x] SUPER_ADMIN puede acceder a cualquier tenant.
- [x] Endpoints públicos funcionan sin JWT.
- [x] `GET /auth/me` retorna datos del usuario autenticado.
- [x] JWKS se cachea y renueva automáticamente. _(AuthFilter con cache de 1h)_

## Testing

### Unitarios

- `AuthFilterTest` — JWT válido, expirado, firma inválida, sin header
- `RoleEnforcementTest` — SUPER_ADMIN acceso total, MERCHANT solo su tenant

## Notas

- En desarrollo, se puede usar un `MockAuthFilter` que acepta un header `X-Mock-Role` para testear sin Cognito.
- El mock se activa solo cuando `env=development` y `auth.mock=true` en config.
- Para login, el FE puede usar directamente la API de Cognito (InitiateAuth) o pasar por el BE como proxy.
