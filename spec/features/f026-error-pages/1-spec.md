# F026 — Páginas de Error y Estados (Backend)

## Responsabilidad del Backend
- Respuestas de error estandarizadas en formato `{ "error": { "code": "...", "message": "..." } }`
- HTTP 404 para recursos no encontrados (productos, órdenes, tenants, categorías)
- HTTP 400 para validaciones fallidas
- HTTP 429 para rate limiting
- HTTP 500 para errores internos (sin exponer detalles al cliente)

## Códigos de Error
| Código | HTTP | Descripción |
|--------|------|-------------|
| NOT_FOUND | 404 | Recurso no encontrado |
| BAD_REQUEST | 400 | Validación fallida |
| RATE_LIMIT_EXCEEDED | 429 | Demasiados requests |
| BUSINESS_RULE | 422 | Regla de negocio violada (stock, etc.) |
| INTERNAL_ERROR | 500 | Error interno del servidor |
