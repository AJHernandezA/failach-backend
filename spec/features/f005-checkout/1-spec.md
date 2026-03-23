# F005 — Checkout / Order Creation API (Backend)

## Propósito

Implementar la creación de órdenes a partir del carrito, validando stock, generando orderCode legible y vaciando el carrito tras éxito.

## Endpoints

| Método | Ruta                                          | Descripción               | Auth               |
| ------ | --------------------------------------------- | ------------------------- | ------------------ |
| `POST` | `/api/v1/tenants/:tenantId/orders`            | Crear orden desde carrito | No (usa sessionId) |
| `GET`  | `/api/v1/tenants/:tenantId/orders/:orderCode` | Obtener detalle de orden  | No                 |

## Domain Layer

### Entidades

**Order**: orderId, orderCode, tenantId, customer (Customer), items (List<OrderItem>), deliveryMethod, deliveryInfo, paymentMethod, paymentStatus, orderStatus, subtotal, shippingCost, total, statusHistory (List<StatusHistory>), notes, createdAt, updatedAt

**Customer** (record): fullName, email, phone

**DeliveryInfo** (record): address, city, neighborhood, additionalInfo

**OrderItem** (record): productId, productName, price, quantity, imageUrl, variantId, variantName

**StatusHistory** (record): status, timestamp, note

### Enums

- **DeliveryMethod**: `SHIPPING`, `PICKUP`
- **PaymentMethod**: `WOMPI`, `BANK_TRANSFER`, `CASH_ON_DELIVERY`
- **PaymentStatus**: `PENDING`, `PAID`, `FAILED`, `REFUNDED`
- **OrderStatus**: `PENDING`, `CONFIRMED`, `PREPARING`, `SHIPPED`, `DELIVERED`, `CANCELLED`

### Ports

**OrderRepository**

- `void save(Order order)`
- `Optional<Order> findByCode(String tenantId, String orderCode)`
- `Page<Order> findByTenant(String tenantId, OrderFilter filter)`
- `void update(Order order)`

### Use Cases

| Interface               | Input                               | Output  |
| ----------------------- | ----------------------------------- | ------- |
| `CreateOrderUseCase`    | `CreateOrderRequest`                | `Order` |
| `GetOrderByCodeUseCase` | `String tenantId, String orderCode` | `Order` |

### Exceptions

| Excepción                    | HTTP | Cuándo                               |
| ---------------------------- | ---- | ------------------------------------ |
| `EmptyCartException`         | 400  | Carrito vacío al intentar checkout   |
| `InsufficientStockException` | 422  | Stock insuficiente de algún producto |
| `OrderNotFoundException`     | 404  | Orden no encontrada por código       |

## Application Layer

### CreateOrderUseCaseImpl

1. Obtener carrito por sessionId.
2. Si carrito vacío → `EmptyCartException`.
3. Para cada item: verificar producto existe, está activo, tiene stock suficiente.
4. Si stock insuficiente → `InsufficientStockException` con detalle del producto.
5. Re-calcular precios desde la BD (nunca confiar en el cliente).
6. Generar `orderCode` legible: `ORD-XXXX-XXXX` (8 chars alfanuméricos).
7. Crear Order con `orderStatus = PENDING`, `paymentStatus = PENDING`.
8. Agregar primer `StatusHistory`: status=PENDING, timestamp=now.
9. Si `paymentMethod = CASH_ON_DELIVERY` → descontar stock inmediatamente.
10. Guardar orden.
11. Vaciar carrito.
12. Retornar orden.

## DynamoDB Access Patterns

| Operación    | PK                         | SK                        |
| ------------ | -------------------------- | ------------------------- |
| Save order   | `TENANT#<id>`              | `ORDER#<orderId>`         |
| Find by code | GSI1: `GSI1PK=TENANT#<id>` | `GSI1SK=ORDERCODE#<code>` |

## Validaciones

| Campo                  | Regla                 | Error                            |
| ---------------------- | --------------------- | -------------------------------- |
| `customer.fullName`    | No vacío, min 3 chars | "Nombre completo es requerido"   |
| `customer.email`       | Email válido          | "Email inválido"                 |
| `customer.phone`       | Formato +57XXXXXXXXXX | "Teléfono inválido"              |
| `deliveryMethod`       | SHIPPING o PICKUP     | "Método de entrega inválido"     |
| `deliveryInfo.address` | Requerido si SHIPPING | "Dirección requerida para envío" |
| `paymentMethod`        | Uno de los 3 válidos  | "Método de pago inválido"        |
| Cart                   | No vacío              | "El carrito está vacío"          |

## Criterios de Aceptación

- [x] `POST /orders` crea orden con orderCode legible.
- [x] Stock insuficiente retorna `422` con detalle del producto.
- [x] Carrito vacío retorna `400`.
- [x] Precios se re-validan contra la BD.
- [x] Carrito se vacía tras crear orden.
- [x] `GET /orders/:orderCode` retorna detalle completo.
- [x] Orden inexistente retorna `404`.
- [x] Efectivo contraentrega descuenta stock al crear.

## Testing

### Unitarios

- `CreateOrderUseCaseImplTest` — creación exitosa, carrito vacío, stock insuficiente, re-validación de precios
- `GetOrderByCodeUseCaseImplTest` — encontrada, no encontrada
