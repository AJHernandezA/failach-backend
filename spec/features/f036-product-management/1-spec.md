# F036 — Product Management CRUD (Backend Spec)

## Endpoints

### `GET /api/v1/admin/products`

Listar productos del tenant autenticado con paginación y filtros.

**Auth**: Requerida (MERCHANT, SUPER_ADMIN).

**Query params**:
| Param | Tipo | Descripción |
|-------|------|-------------|
| `tenantId` | String | Solo SUPER_ADMIN; MERCHANT auto-filtra |
| `categoryId` | String | Filtrar por categoría |
| `status` | String | `active`, `inactive`, `all` (default: `all`) |
| `search` | String | Búsqueda por nombre |
| `page` | Number | Página (default: 1) |
| `limit` | Number | Items por página (default: 20, max: 50) |
| `sort` | String | `name_asc`, `name_desc`, `price_asc`, `price_desc`, `created_desc` |

**Response** (`200 OK`):
```json
{
  "items": [
    {
      "id": "prod-001",
      "name": "Bowl Energético",
      "description": "Bowl con frutas...",
      "price": 25000,
      "discountPrice": null,
      "categoryId": "cat-bowls",
      "categoryName": "Bowls",
      "stock": 50,
      "images": ["https://cdn.../bowl.jpg"],
      "featured": true,
      "active": true,
      "createdAt": "2026-01-15T10:00:00Z",
      "updatedAt": "2026-03-10T14:30:00Z"
    }
  ],
  "total": 28,
  "page": 1,
  "limit": 20,
  "totalPages": 2
}
```

### `POST /api/v1/admin/products`

Crear nuevo producto.

**Auth**: Requerida (MERCHANT, SUPER_ADMIN).

**Request body**:
```json
{
  "name": "Bowl Energético",
  "description": "Bowl con frutas frescas, granola y miel",
  "price": 25000,
  "discountPrice": 22000,
  "categoryId": "cat-bowls",
  "stock": 50,
  "images": ["https://cdn.../bowl1.jpg", "https://cdn.../bowl2.jpg"],
  "featured": true,
  "active": true,
  "weight": 350,
  "variants": [
    { "key": "Tamaño", "values": ["Regular", "Grande"] }
  ]
}
```

**Response** (`201 Created`): Producto creado con `id` generado.

**Validaciones**:
| Campo | Regla |
|-------|-------|
| `name` | Requerido, 2-200 chars |
| `description` | Requerido, 10-2000 chars |
| `price` | Requerido, > 0 |
| `discountPrice` | Opcional, > 0 y < price |
| `categoryId` | Requerido, debe existir para el tenant |
| `stock` | Requerido, >= 0 |
| `images` | Requerido, 1-10 URLs válidas |
| `weight` | Opcional, > 0 |

### `GET /api/v1/admin/products/:id`

Obtener producto por ID.

**Auth**: Requerida. Verifica que el producto pertenezca al tenant del MERCHANT.

### `PUT /api/v1/admin/products/:id`

Actualizar producto existente.

**Auth**: Requerida. Solo el tenant propietario (o SUPER_ADMIN).

**Request body**: Campos parciales (solo los que se quieren actualizar).

### `DELETE /api/v1/admin/products/:id`

Eliminar producto (soft delete).

**Auth**: Requerida. Solo el tenant propietario (o SUPER_ADMIN).

**Implementación**: Marca `deleted=true` y `active=false`. No elimina de DynamoDB.

**Response** (`200 OK`):
```json
{ "message": "Producto eliminado" }
```

### `GET /api/v1/admin/categories`

Listar categorías del tenant.

**Auth**: Requerida.

### `POST /api/v1/admin/categories`

Crear nueva categoría.

**Auth**: Requerida.

**Request body**:
```json
{
  "name": "Bowls",
  "description": "Bowls saludables"
}
```

### `POST /api/v1/admin/upload/presigned-url`

Generar pre-signed URL para upload directo a S3.

**Auth**: Requerida.

**Request body**:
```json
{
  "filename": "bowl-energetico.jpg",
  "contentType": "image/jpeg"
}
```

**Response** (`200 OK`):
```json
{
  "uploadUrl": "https://s3.amazonaws.com/projectx-assets-prod/...",
  "cdnUrl": "https://cdn.projectx.com/tenants/idoneo/products/bowl-energetico-uuid.jpg",
  "expiresIn": 300
}
```

**Implementación**:
- Genera key en S3: `tenants/<tenantId>/products/<uuid>-<filename>`.
- Genera pre-signed PUT URL con TTL de 5 min.
- Retorna la URL de CDN correspondiente (CloudFront).
- Valida contentType: solo `image/jpeg`, `image/png`, `image/webp`.

## Arquitectura (Hexagonal)

```
infrastructure/
├── web/
│   ├── AdminProductController.java
│   ├── AdminCategoryController.java
│   └── AdminUploadController.java
├── persistence/
│   ├── DynamoAdminProductRepository.java
│   └── DynamoAdminCategoryRepository.java
├── storage/
│   └── S3StorageService.java
application/
├── usecase/
│   ├── CreateProductUseCase.java
│   ├── UpdateProductUseCase.java
│   ├── DeleteProductUseCase.java
│   ├── ListAdminProductsUseCase.java
│   ├── GetAdminProductUseCase.java
│   ├── CreateCategoryUseCase.java
│   ├── ListAdminCategoriesUseCase.java
│   └── GeneratePresignedUrlUseCase.java
domain/
├── model/
│   ├── Product.java           // (ya existe, se extiende)
│   └── Category.java          // (ya existe)
├── port/
│   ├── ProductRepository.java // (se extiende con métodos admin)
│   ├── CategoryRepository.java
│   └── StorageService.java    // Puerto para S3
```

## DynamoDB (extensión del modelo existente)

Los productos ya existen en F003. Se agregan campos:

| Campo nuevo | Tipo | Descripción |
|------------|------|-------------|
| `discountPrice` | Number | Precio con descuento |
| `featured` | Boolean | Producto destacado |
| `weight` | Number | Peso en gramos |
| `variants` | Map | Variantes (key → values) |
| `deleted` | Boolean | Soft delete |
| `updatedAt` | String | Última actualización |

## Diferencia con endpoints públicos (F003)

| Aspecto | F003 (público) | F036 (admin) |
|---------|---------------|--------------|
| Auth | No | Sí (JWT) |
| Filtro | Solo active=true, deleted=false | Todos |
| Operaciones | Solo lectura | CRUD completo |
| Campos | Campos públicos | Todos los campos |
| Ruta | `/tenants/:tenantId/products` | `/admin/products` |

## Comentarios en código

Sí, todos los archivos Java deben tener comentarios explicatorios en español.

## Notas

- El `tenantId` para MERCHANT se extrae automáticamente del JWT — nunca se confía en un parámetro del request.
- SUPER_ADMIN puede pasar `tenantId` como query param para gestionar productos de cualquier tienda.
- El soft delete preserva los datos para historial de pedidos (las órdenes pasadas referencian estos productos).
- Las imágenes se suben directamente a S3 via pre-signed URL — el backend solo genera la URL firmada.
- La generación de pre-signed URL valida el contentType y genera un path seguro en S3.
