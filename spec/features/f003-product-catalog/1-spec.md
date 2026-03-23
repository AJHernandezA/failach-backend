# F003 — Product Catalog API (Backend)

## Propósito

Implementar el CRUD completo de productos y categorías con paginación, filtros y búsqueda.

## Endpoints

| Método   | Ruta                                            | Descripción                            | Auth                   |
| -------- | ----------------------------------------------- | -------------------------------------- | ---------------------- |
| `GET`    | `/api/v1/tenants/:tenantId/products`            | Listar productos (paginado, filtrable) | No                     |
| `GET`    | `/api/v1/tenants/:tenantId/products/:productId` | Detalle de producto                    | No                     |
| `POST`   | `/api/v1/tenants/:tenantId/products`            | Crear producto                         | SUPER_ADMIN / MERCHANT |
| `PUT`    | `/api/v1/tenants/:tenantId/products/:productId` | Actualizar producto                    | SUPER_ADMIN / MERCHANT |
| `DELETE` | `/api/v1/tenants/:tenantId/products/:productId` | Eliminar producto (soft delete)        | SUPER_ADMIN / MERCHANT |
| `GET`    | `/api/v1/tenants/:tenantId/categories`          | Listar categorías                      | No                     |
| `POST`   | `/api/v1/tenants/:tenantId/categories`          | Crear categoría                        | SUPER_ADMIN / MERCHANT |

## Domain Layer

### Entidades

**Product** (record): productId, tenantId, name, description, price (BigDecimal), compareAtPrice, images (List<String>), categoryId, categoryName, stock (int), variants (List<ProductVariant>), status (ProductStatus), sortOrder, createdAt, updatedAt

**ProductVariant** (record): variantId, name, price, stock

**Category** (record): categoryId, tenantId, name, description, imageUrl, sortOrder

### Enums

**ProductStatus**: `ACTIVE`, `INACTIVE`, `OUT_OF_STOCK`

### Ports

**ProductRepository**

- `Optional<Product> findById(String tenantId, String productId)`
- `Page<Product> findByTenant(String tenantId, ProductFilter filter)`
- `List<Product> findByCategory(String tenantId, String categoryId)`
- `void save(Product product)`
- `void delete(String tenantId, String productId)`

**CategoryRepository**

- `List<Category> findByTenant(String tenantId)`
- `Optional<Category> findById(String tenantId, String categoryId)`
- `void save(Category category)`

### Use Cases

| Interface               | Input                               | Output           |
| ----------------------- | ----------------------------------- | ---------------- |
| `ListProductsUseCase`   | `String tenantId, ProductFilter`    | `Page<Product>`  |
| `GetProductUseCase`     | `String tenantId, String productId` | `Product`        |
| `CreateProductUseCase`  | `CreateProductRequest`              | `Product`        |
| `UpdateProductUseCase`  | `UpdateProductRequest`              | `Product`        |
| `DeleteProductUseCase`  | `String tenantId, String productId` | `void`           |
| `ListCategoriesUseCase` | `String tenantId`                   | `List<Category>` |
| `CreateCategoryUseCase` | `CreateCategoryRequest`             | `Category`       |

### ProductFilter (record)

- `categoryId`: String (opcional)
- `search`: String (opcional, búsqueda por nombre)
- `status`: ProductStatus (opcional, default ACTIVE para público)
- `page`: int (default 0)
- `size`: int (default 20)

## DynamoDB Access Patterns

| Operación               | PK            | SK                                      |
| ----------------------- | ------------- | --------------------------------------- |
| Listar productos        | `TENANT#<id>` | begins_with `PRODUCT#`                  |
| Producto por ID         | `TENANT#<id>` | `PRODUCT#<productId>`                   |
| Productos por categoría | `TENANT#<id>` | begins_with `PRODUCT#CAT#<categoryId>#` |
| Listar categorías       | `TENANT#<id>` | begins_with `CATEGORY#`                 |

## Validaciones

| Campo        | Regla                   | Error                              |
| ------------ | ----------------------- | ---------------------------------- |
| `name`       | No vacío, max 200 chars | "Nombre del producto es requerido" |
| `price`      | > 0                     | "El precio debe ser mayor a 0"     |
| `images`     | Al menos 1              | "Se requiere al menos una imagen"  |
| `categoryId` | Debe existir            | "La categoría no existe"           |
| `stock`      | >= 0                    | "El stock no puede ser negativo"   |

## Criterios de Aceptación

- [x] `GET /products` retorna lista paginada con 20 items por defecto.
- [x] `GET /products?category=X` filtra por categoría.
- [x] `GET /products?search=arroz` busca por nombre.
- [x] `GET /products/:id` retorna detalle completo.
- [x] `POST /products` crea producto (auth requerida).
- [x] `PUT /products/:id` actualiza producto (auth requerida).
- [x] `DELETE /products/:id` hace soft delete (status → INACTIVE).
- [x] Producto con stock 0 tiene status `OUT_OF_STOCK`.
- [x] Solo productos `ACTIVE` se muestran en GET público.

## Testing

### Unitarios

- `ListProductsUseCaseImplTest` — paginación, filtros, búsqueda
- `CreateProductUseCaseImplTest` — creación exitosa, validaciones
- `GetProductUseCaseImplTest` — encontrado, no encontrado

### Integración

- `ProductControllerIntegrationTest` — todos los endpoints
