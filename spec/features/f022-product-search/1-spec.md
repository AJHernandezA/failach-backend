# F022 — Product Search (Backend)

## Propósito

Endpoint de búsqueda de productos con filtros por texto, categoría, rango de precio y ordenamiento. Para MVP, implementación simple con DynamoDB Query/Scan + filtros en aplicación.

## Endpoints

| Método | Ruta                                                | Descripción                    | Auth |
| ------ | --------------------------------------------------- | ------------------------------ | ---- |
| `GET`  | `/api/v1/tenants/:tenantId/products/search`         | Buscar productos con filtros   | No   |

### Query Parameters

| Param      | Tipo    | Requerido | Descripción                                          |
| ---------- | ------- | --------- | ---------------------------------------------------- |
| `q`        | String  | Sí        | Término de búsqueda (mínimo 2 caracteres)            |
| `category` | String  | No        | ID de categoría para filtrar                         |
| `minPrice` | Integer | No        | Precio mínimo en centavos                            |
| `maxPrice` | Integer | No        | Precio máximo en centavos                            |
| `sort`     | String  | No        | `price_asc`, `price_desc`, `name`, `newest` (default: `name`) |
| `page`     | Integer | No        | Página (default: 1)                                  |
| `size`     | Integer | No        | Resultados por página (default: 20, max: 50)         |

## Domain Layer

### Use Cases

| Interface                | Input                       | Output                    |
| ------------------------ | --------------------------- | ------------------------- |
| `SearchProductsUseCase`  | `SearchProductsRequest`     | `PaginatedResult<Product>`|

### SearchProductsRequest (record)

```
tenantId: String
query: String (min 2 chars)
categoryId: String (nullable)
minPrice: Integer (nullable)
maxPrice: Integer (nullable)
sort: SortOption (enum)
page: int (default 1)
size: int (default 20, max 50)
```

### SortOption (enum)

- `PRICE_ASC` — Precio menor a mayor
- `PRICE_DESC` — Precio mayor a menor
- `NAME` — Nombre alfabético
- `NEWEST` — Más reciente primero

## Application Layer

### SearchProductsUseCaseImpl

1. Validar query (mínimo 2 caracteres).
2. Obtener todos los productos activos del tenant (DynamoDB Query con PK=`TENANT#<id>`, SK begins_with `PRODUCT#`).
3. Filtrar en memoria:
   - Nombre o descripción contiene `query` (case-insensitive).
   - Si `categoryId` → filtrar por categoría.
   - Si `minPrice` → filtrar precio >= minPrice.
   - Si `maxPrice` → filtrar precio <= maxPrice.
4. Ordenar según `sort`.
5. Paginar resultados (offset-based para simplicidad en MVP).
6. Retornar `PaginatedResult` con items, total, page, totalPages.

## DynamoDB Access Patterns

| Operación         | PK            | SK                      | Filtro                      |
| ----------------- | ------------- | ----------------------- | --------------------------- |
| Query products    | `TENANT#<id>` | `begins_with(PRODUCT#)` | En aplicación (nombre, etc) |

## Response

```json
{
  "data": {
    "items": [
      {
        "id": "prod-001",
        "name": "Bowl Energético",
        "description": "Bowl con granola, frutas...",
        "price": 28500,
        "images": ["https://cdn.projectx.com/..."],
        "categoryId": "cat-001",
        "categoryName": "Bowls"
      }
    ],
    "total": 15,
    "page": 1,
    "totalPages": 1,
    "size": 20
  }
}
```

## Criterios de Aceptación

- [ ] Búsqueda por nombre funciona (case-insensitive, contains).
- [ ] Búsqueda por descripción funciona.
- [ ] Filtro por categoría funciona.
- [ ] Filtro por rango de precio funciona.
- [ ] Ordenamiento funciona (4 opciones).
- [ ] Paginación retorna resultados correctos.
- [ ] Query vacío o < 2 caracteres retorna `400`.
- [ ] `size` > 50 se limita a 50.
- [ ] Solo retorna productos activos (`status = ACTIVE`).

## Notas

- Estrategia MVP: Query todos los productos del tenant + filtro en memoria. Esto funciona bien para catálogos < 1000 productos.
- Si un tenant crece mucho, se puede optimizar con GSI (Global Secondary Index) en DynamoDB o migrar a OpenSearch.
- El rate limiting de F019 protege contra abuso de este endpoint.
- Considerar cachear los productos en memoria por tenant (TTL 5 min) para reducir lecturas a DynamoDB.
