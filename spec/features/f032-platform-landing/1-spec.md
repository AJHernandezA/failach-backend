# F032 — Platform Landing Page (Backend Spec)

## Endpoints

### `GET /api/v1/platform/info`

Retorna estadísticas públicas de la plataforma.

**Auth**: No requerida.

**Response** (`200 OK`):
```json
{
  "totalStores": 3,
  "totalProducts": 45,
  "totalOrders": 120
}
```

**Implementación**:
- `PlatformController.getInfo()` → `GetPlatformInfoUseCase`
- Cuenta tenants con status=ACTIVE, productos activos totales, pedidos completados totales.
- Cachear resultado en memoria (TTL 5 min) para evitar queries frecuentes.

### `GET /api/v1/platform/stores`

Retorna lista pública de tiendas activas con datos de preview.

**Auth**: No requerida.

**Query params**: `category` (opcional), `search` (opcional), `sort` (opcional: `recent`, `popular`).

**Response** (`200 OK`):
```json
[
  {
    "tenantId": "idoneo",
    "businessName": "IDONEO",
    "category": "Restaurante Saludable",
    "description": "Comida saludable y deliciosa en Barranquilla",
    "logoUrl": "https://cdn.../idoneo/logo.png",
    "storeUrl": "https://idoneo.projectx.com",
    "totalProducts": 15
  }
]
```

**Implementación**:
- `PlatformController.getStores()` → `ListPublicStoresUseCase`
- Solo retorna tenants con status=ACTIVE.
- Filtra por categoría si se pasa `category`.
- Búsqueda case-insensitive por nombre si se pasa `search`.

### `POST /api/v1/platform/contact`

Recibe mensaje de contacto del formulario de la landing.

**Auth**: No requerida.

**Request body**:
```json
{
  "name": "Juan Pérez",
  "email": "juan@test.com",
  "phone": "+573001234567",
  "message": "Me interesa la plataforma para mi negocio..."
}
```

**Response** (`201 Created`):
```json
{
  "id": "msg-uuid",
  "message": "Mensaje enviado exitosamente"
}
```

**Implementación**:
- `PlatformController.sendContactMessage()` → `SendContactMessageUseCase`
- Valida campos (nombre, email requeridos; message min 10 chars).
- Guarda en DynamoDB (PK=`PLATFORM#CONTACT`, SK=`MSG#<timestamp>#<id>`).
- Opcionalmente envía notificación por email al admin (adolfohernandezaponte@hotmail.com).
- Rate limiting: máximo 3 mensajes/hora por IP.

## Arquitectura (Hexagonal)

```
infrastructure/
├── web/
│   └── PlatformController.java        // Endpoints REST
application/
├── usecase/
│   ├── GetPlatformInfoUseCase.java     // Obtener stats
│   ├── ListPublicStoresUseCase.java    // Listar tiendas públicas
│   └── SendContactMessageUseCase.java  // Enviar mensaje de contacto
domain/
├── model/
│   ├── PlatformInfo.java               // Stats de la plataforma
│   ├── StorePreview.java               // Preview de tienda
│   └── ContactMessage.java             // Mensaje de contacto
├── port/
│   └── PlatformRepository.java         // Puerto de acceso a datos
infrastructure/
├── persistence/
│   └── DynamoPlatformRepository.java   // Implementación DynamoDB
```

## DynamoDB

| PK | SK | Datos |
|----|-----|-------|
| `PLATFORM#CONTACT` | `MSG#<timestamp>#<id>` | name, email, phone, message, createdAt, ip |
| `PLATFORM#STATS` | `STATS#CURRENT` | totalStores, totalProducts, totalOrders, updatedAt |

## Validaciones

| Campo | Regla |
|-------|-------|
| `name` | Requerido, 2-100 chars, sanitizado |
| `email` | Requerido, formato email válido |
| `phone` | Opcional, formato colombiano (+57...) |
| `message` | Requerido, 10-1000 chars, sanitizado |

## Comentarios en código

Sí, todos los archivos Java deben tener comentarios explicatorios en español.

## Notas

- El endpoint `/platform/info` es público y ligero — no expone datos sensibles.
- Las stats se pueden pre-calcular y cachear para evitar scans costosos en DynamoDB.
- El rate limiting de contacto previene spam (3 msg/hora/IP).
- Los mensajes de contacto se guardan para revisión manual.
