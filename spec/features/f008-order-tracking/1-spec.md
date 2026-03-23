# F008 — Order Tracking API (Backend)

## Propósito

Implementar la actualización de estados de órdenes con validación de transiciones y el historial de cambios.

## Endpoints

| Método | Ruta                                                 | Descripción                          | Auth                   |
| ------ | ---------------------------------------------------- | ------------------------------------ | ---------------------- |
| `GET`  | `/api/v1/tenants/:tenantId/orders/:orderCode`        | Detalle de orden por código          | No                     |
| `PUT`  | `/api/v1/tenants/:tenantId/orders/:orderCode/status` | Actualizar estado                    | SUPER_ADMIN / MERCHANT |
| `GET`  | `/api/v1/tenants/:tenantId/orders`                   | Listar órdenes del tenant (paginado) | SUPER_ADMIN / MERCHANT |

## Domain Layer

### Use Cases

| Interface                  | Input                          | Output        |
| -------------------------- | ------------------------------ | ------------- |
| `UpdateOrderStatusUseCase` | `UpdateStatusRequest`          | `Order`       |
| `ListOrdersUseCase`        | `String tenantId, OrderFilter` | `Page<Order>` |

### Transiciones Válidas

```
PENDING → CONFIRMED        (tras pago)
CONFIRMED → PREPARING      (comercio prepara)
PREPARING → SHIPPED        (comercio envía)
SHIPPED → DELIVERED        (entregado)
* → CANCELLED              (excepto DELIVERED)
```

### OrderFilter (record)

- `status`: OrderStatus (opcional)
- `paymentStatus`: PaymentStatus (opcional)
- `page`: int (default 0)
- `size`: int (default 20)

## Application Layer

### UpdateOrderStatusUseCaseImpl

1. Buscar orden.
2. Validar transición: verificar que el nuevo estado es alcanzable desde el actual.
3. Si transición inválida → `BusinessRuleException` con mensaje descriptivo.
4. Actualizar `orderStatus`.
5. Agregar `StatusHistory` con timestamp y nota opcional.
6. Guardar orden.
7. Disparar notificación email (F009).
8. Retornar orden actualizada.

### Tabla de Transiciones (validación)

| Estado Actual | Transiciones Permitidas  |
| ------------- | ------------------------ |
| `PENDING`     | `CONFIRMED`, `CANCELLED` |
| `CONFIRMED`   | `PREPARING`, `CANCELLED` |
| `PREPARING`   | `SHIPPED`, `CANCELLED`   |
| `SHIPPED`     | `DELIVERED`, `CANCELLED` |
| `DELIVERED`   | (ninguna — estado final) |
| `CANCELLED`   | (ninguna — estado final) |

## DynamoDB Access Patterns

| Operación         | PK                  | SK / GSI                             |
| ----------------- | ------------------- | ------------------------------------ |
| Find by orderCode | GSI1: `TENANT#<id>` | `ORDERCODE#<code>`                   |
| List by tenant    | `TENANT#<id>`       | begins_with `ORDER#`                 |
| List by status    | `TENANT#<id>`       | begins_with `ORDER#STATUS#<status>#` |

## Criterios de Aceptación

- [x] `GET /orders/:orderCode` retorna orden con statusHistory completo.
- [x] `PUT /orders/:orderCode/status` actualiza estado (auth requerida).
- [x] Transición inválida retorna `422` con mensaje: "No se puede cambiar de X a Y".
- [x] `GET /orders` lista órdenes con paginación y filtros (auth requerida).
- [x] Cada cambio de estado agrega entrada a statusHistory con timestamp.
- [x] No se puede modificar una orden DELIVERED o CANCELLED.

## Testing

### Unitarios

- `UpdateOrderStatusUseCaseImplTest` — 10 tests: PENDING→CONFIRMED, CONFIRMED→PREPARING, PREPARING→SHIPPED, SHIPPED→DELIVERED, cancel desde PENDING/CONFIRMED, transiciones inválidas (PENDING→PREPARING, DELIVERED→*, CANCELLED→*), orden no encontrada
- `ListOrdersUseCaseImplTest` — 2 tests: paginación con resultados, lista vacía
