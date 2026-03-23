# F034 — Admin Dashboard Layout (Backend Spec)

## Alcance Backend

Este feature es principalmente frontend (layout, sidebar, navigation). El backend solo necesita:

1. **AuthFilter** ya implementado en F033 — protege rutas `/api/v1/admin/**`.
2. **RoleFilter** ya implementado en F033 — verifica SUPER_ADMIN o MERCHANT.
3. **`GET /api/v1/auth/me`** ya implementado en F033 — retorna usuario con rol para que el FE determine qué sidebar items mostrar.

## No requiere endpoints nuevos

El layout del admin se construye enteramente en el frontend usando la información del usuario autenticado (`role`, `tenantId`) obtenida de `GET /api/v1/auth/me`.

## Rutas protegidas

Todas las rutas bajo `/api/v1/admin/**` requieren:
- Token JWT válido (AuthFilter)
- Rol SUPER_ADMIN o MERCHANT (RoleFilter)

Las rutas específicas de SUPER_ADMIN se protegen adicionalmente:
- `GET /api/v1/admin/stores` → solo SUPER_ADMIN
- `PUT /api/v1/admin/stores/:tenantId/status` → solo SUPER_ADMIN
- `GET /api/v1/admin/reports/stores` → solo SUPER_ADMIN

## Comentarios en código

Sí, todos los archivos Java deben tener comentarios explicatorios en español.

## Notas

- El backend no conoce ni controla el layout del frontend.
- La responsabilidad del backend es proveer la info del usuario (rol, tenant) y proteger los endpoints según rol.
- Los endpoints admin filtran datos automáticamente: si el usuario es MERCHANT, solo retorna datos de su tenant.
