# F004 — Shopping Cart API (Backend)

## Propósito

Implementar la API del carrito de compras con gestión de sesión, CRUD de items y validación de stock.

## Endpoints

| Método   | Ruta                                              | Descripción                   | Auth                  |
| -------- | ------------------------------------------------- | ----------------------------- | --------------------- |
| `GET`    | `/api/v1/tenants/:tenantId/cart`                  | Obtener carrito actual        | No (cookie sessionId) |
| `POST`   | `/api/v1/tenants/:tenantId/cart/items`            | Agregar producto al carrito   | No                    |
| `PUT`    | `/api/v1/tenants/:tenantId/cart/items/:productId` | Actualizar cantidad           | No                    |
| `DELETE` | `/api/v1/tenants/:tenantId/cart/items/:productId` | Eliminar producto del carrito | No                    |
| `DELETE` | `/api/v1/tenants/:tenantId/cart`                  | Vaciar carrito                | No                    |

## Domain Layer

### Entidades

**Cart** (record): cartId (= sessionId), tenantId, items (List<CartItem>), createdAt, updatedAt, ttl (Long)

**CartItem** (record): productId, productName, price (BigDecimal), quantity (int), imageUrl, variantId, variantName

### Ports

**CartRepository**

- `Optional<Cart> findById(String tenantId, String sessionId)`
- `void save(Cart cart)`
- `void delete(String tenantId, String sessionId)`

### Use Cases

| Interface               | Input                                                 | Output |
| ----------------------- | ----------------------------------------------------- | ------ |
| `GetCartUseCase`        | `String tenantId, String sessionId`                   | `Cart` |
| `AddToCartUseCase`      | `AddToCartRequest`                                    | `Cart` |
| `UpdateCartItemUseCase` | `UpdateCartItemRequest`                               | `Cart` |
| `RemoveCartItemUseCase` | `String tenantId, String sessionId, String productId` | `Cart` |
| `ClearCartUseCase`      | `String tenantId, String sessionId`                   | `void` |

## Session Management

1. Primera vez que el usuario interactúa con el carrito: BE genera un `sessionId` (UUID).
2. Lo devuelve como cookie httpOnly: `Set-Cookie: sessionId=<uuid>; HttpOnly; Secure; SameSite=Strict; Path=/; Max-Age=86400`.
3. Requests subsiguientes envían la cookie automáticamente.
4. El carrito se identifica por `tenantId + sessionId`.
5. TTL de 24 horas en DynamoDB para carritos abandonados.

## Validaciones

| Regla                                  | Error                                    |
| -------------------------------------- | ---------------------------------------- |
| Producto debe existir y estar activo   | "Producto no encontrado o no disponible" |
| Cantidad >= 1                          | "La cantidad mínima es 1"                |
| Cantidad <= stock disponible           | "Stock insuficiente. Disponible: X"      |
| No agregar duplicados (sumar cantidad) | — (no es error, se suma)                 |

## DynamoDB Access Patterns

| Operación   | PK            | SK                 | TTL      |
| ----------- | ------------- | ------------------ | -------- |
| Get cart    | `TENANT#<id>` | `CART#<sessionId>` | Sí (24h) |
| Save cart   | `TENANT#<id>` | `CART#<sessionId>` | Sí (24h) |
| Delete cart | `TENANT#<id>` | `CART#<sessionId>` | —        |

## Criterios de Aceptación

- [x] POST crea carrito si no existe y agrega producto.
- [x] POST a carrito existente agrega nuevo item o suma cantidad.
- [x] GET retorna carrito con todos los items.
- [x] PUT actualiza cantidad de un item.
- [x] DELETE item elimina un producto del carrito.
- [x] DELETE cart vacía todo el carrito.
- [x] Producto inexistente retorna `404`.
- [x] Stock insuficiente retorna `422`.
- [x] Cookie `sessionId` se setea correctamente.
- [x] Cart expira después de 24h (TTL en DynamoDB).

## Testing

### Unitarios

- `AddToCartUseCaseImplTest` — agregar nuevo, agregar existente, stock insuficiente
- `UpdateCartItemUseCaseImplTest` — actualizar cantidad, validaciones
- `GetCartUseCaseImplTest` — carrito existente, carrito nuevo (vacío)
