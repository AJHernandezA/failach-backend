# [CÓDIGO] — Ejemplos de API (Backend)

## [Nombre del caso] — Éxito

Request:
```
[METHOD] /api/v1/tenants/[tenantId]/[recurso]
Headers:
  X-Tenant-Id: [tenantId]
  Authorization: Bearer [token]       (si aplica)
  Content-Type: application/json      (si aplica)

Body:                                  (si aplica)
{
  "[campo1]": "[valor1]",
  "[campo2]": "[valor2]"
}
```

Response: `[status code]`
```json
{
  "data": {
    "[campo1]": "[valor1]",
    "[campo2]": "[valor2]"
  }
}
```

---

## [Nombre del caso] — Error de Validación

Request:
```
[METHOD] /api/v1/tenants/[tenantId]/[recurso]
Headers:
  X-Tenant-Id: [tenantId]

Body:
{
  "[campo1]": ""
}
```

Response: `400 Bad Request`
```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "[campo1] es requerido"
  }
}
```

---

## [Nombre del caso] — No encontrado

Request:
```
GET /api/v1/tenants/[tenantId]/[recurso]/inexistente
Headers:
  X-Tenant-Id: [tenantId]
```

Response: `404 Not Found`
```json
{
  "error": {
    "code": "NOT_FOUND",
    "message": "[Recurso] con id 'inexistente' no encontrado"
  }
}
```

---

## [Nombre del caso] — Sin autorización

Request:
```
[METHOD] /api/v1/tenants/[tenantId]/[recurso]
Headers:
  X-Tenant-Id: [tenantId]
  (sin Authorization header)
```

Response: `401 Unauthorized`
```json
{
  "error": {
    "code": "UNAUTHORIZED",
    "message": "Token de autenticación requerido"
  }
}
```

---

## [Nombre del caso] — Sin permisos

Request:
```
[METHOD] /api/v1/tenants/[tenantId]/[recurso]
Headers:
  X-Tenant-Id: tenantA
  Authorization: Bearer [token_de_tenantB]
```

Response: `403 Forbidden`
```json
{
  "error": {
    "code": "FORBIDDEN",
    "message": "No tienes permisos para acceder a este recurso"
  }
}
```
