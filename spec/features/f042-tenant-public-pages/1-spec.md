# F042 — Tenant Public Pages & Store Directory (Backend Spec)

## Endpoints

### `GET /api/v1/platform/stores`

Ya definido en F032. Listar tiendas activas con datos de preview.

**Auth**: No requerida.

**Query params**:
| Param | Tipo | Descripción |
|-------|------|-------------|
| `category` | String | Filtrar por categoría de negocio |
| `search` | String | Buscar por nombre |
| `sort` | String | `recent`, `popular` (default: `recent`) |

### `GET /api/v1/platform/stores/:tenantId`

Perfil público completo de una tienda.

**Auth**: No requerida.

**Response** (`200 OK`):
```json
{
  "tenantId": "idoneo",
  "businessName": "IDONEO",
  "category": "Restaurante Saludable",
  "description": "Comida saludable y deliciosa en Barranquilla...",
  "slogan": "Alimenta tu cuerpo, nutre tu alma",
  "logoUrl": "https://cdn.../idoneo/logo.png",
  "bannerUrl": "https://cdn.../idoneo/banner.jpg",
  "phone": "+573145429669",
  "whatsapp": "+573145429669",
  "email": "contacto@idoneo.co",
  "address": "Calle 123 #45-67, Barranquilla",
  "socialMedia": {
    "instagram": "https://instagram.com/idoneo_co",
    "facebook": null,
    "tiktok": null
  },
  "coverageCities": ["Barranquilla", "Soledad"],
  "schedule": {
    "monday": "8:00 - 20:00",
    "tuesday": "8:00 - 20:00",
    "wednesday": "8:00 - 20:00",
    "thursday": "8:00 - 20:00",
    "friday": "8:00 - 20:00",
    "saturday": "9:00 - 18:00",
    "sunday": "Cerrado"
  },
  "featuredProducts": [
    {
      "id": "prod-001",
      "name": "Bowl Energético",
      "price": 25000,
      "imageUrl": "https://cdn.../bowl.jpg"
    }
  ],
  "categories": ["Bowls", "Jugos", "Snacks"],
  "storeUrl": "https://idoneo.projectx.com",
  "totalProducts": 15,
  "status": "ACTIVE"
}
```

**Implementación**:
- `PlatformController.getStoreProfile()` → `GetStorePublicProfileUseCase`
- Obtiene config del tenant (F002).
- Obtiene primeros 6 productos featured (F003).
- Obtiene categorías del tenant.
- Solo retorna si status=ACTIVE. Si PENDING o SUSPENDED → 404.

**Errores**:
- `404 Not Found` si el tenantId no existe o no está activo.

## Arquitectura (Hexagonal)

```
infrastructure/
├── web/
│   └── PlatformController.java        // (extensión de F032)
application/
├── usecase/
│   ├── ListPublicStoresUseCase.java   // (ya existe en F032)
│   └── GetStorePublicProfileUseCase.java  // Nuevo
domain/
├── model/
│   └── StorePublicProfile.java        // Nuevo
├── port/
│   └── PlatformRepository.java        // (extensión)
```

## DynamoDB Queries

### Para el perfil público:
1. `TENANT#<tenantId> | CONFIG` → configuración del tenant
2. `TENANT#<tenantId> | PRODUCT#*` → productos con `featured=true` y `active=true`, limit 6
3. `TENANT#<tenantId> | CATEGORY#*` → categorías del tenant

### Para el listado:
1. Scan de todos los tenants con `status=ACTIVE`
2. Filtrar por categoría si se pasa
3. Búsqueda case-insensitive por nombre
4. Ordenar por fecha de creación (recent) o por totalOrders (popular)

## Cache

- El perfil público de una tienda se puede cachear (TTL 5 min) ya que no cambia frecuentemente.
- El listado de tiendas activas también (TTL 5 min).

## Comentarios en código

Sí, todos los archivos Java deben tener comentarios explicatorios en español.

## Notas

- Este endpoint es público — no requiere autenticación.
- Solo muestra tiendas con status=ACTIVE.
- Los datos sensibles (datos bancarios, emails de admin) NO se incluyen en la respuesta pública.
- El `storeUrl` se construye dinámicamente: `https://<tenantId>.<domain>`.
- Los `featuredProducts` son un subset de los productos del tenant (máximo 6).
