# F037 — Order Management Dashboard (Backend Spec)

## Endpoints

### `GET /api/v1/admin/orders`

Listar pedidos con filtros y paginación.

**Auth**: Requerida (MERCHANT, SUPER_ADMIN).

**Query params**:
| Param | Tipo | Descripción |
|-------|------|-------------|
| `tenantId` | String | Solo SUPER_ADMIN |
| `status` | String | Filtrar por estado |
| `paymentMethod` | String | Filtrar por método de pago |
| `from` | String | Fecha inicio ISO 8601 |
| `to` | String | Fecha fin ISO 8601 |
| `search` | String | Buscar por código o nombre cliente |
| `page` | Number | Página (default: 1) |
| `limit` | Number | Items por página (default: 20) |
| `sort` | String | `date_desc` (default), `date_asc`, `amount_desc`, `amount_asc` |

**Response** (`200 OK`):
```json
{
  "items": [
    {
      "orderCode": "ORD-2026-0045",
      "customerName": "Juan Pérez",
      "customerPhone": "+573001234567",
      "customerEmail": "juan@test.com",
      "itemsSummary": "Bowl Energético x2, Jugo Verde x1",
      "totalAmount": 63900,
      "paymentMethod": "BANK_TRANSFER",
      "status": "CONFIRMED",
      "isScheduled": false,
      "createdAt": "2026-03-15T10:30:00Z"
    }
  ],
  "total": 45,
  "page": 1,
  "limit": 20,
  "totalPages": 3
}
```

### `GET /api/v1/admin/orders/:code`

Detalle completo de un pedido.

**Auth**: Requerida.

**Response** (`200 OK`):
```json
{
  "orderCode": "ORD-2026-0045",
  "tenantId": "idoneo",
  "customer": {
    "name": "Juan Pérez",
    "email": "juan@test.com",
    "phone": "+573001234567",
    "city": "Barranquilla",
    "address": "Calle 123 #45-67"
  },
  "items": [
    {
      "productId": "prod-001",
      "name": "Bowl Energético",
      "imageUrl": "https://cdn.../bowl.jpg",
      "price": 25000,
      "quantity": 2,
      "subtotal": 50000
    }
  ],
  "payment": {
    "method": "BANK_TRANSFER",
    "status": "CONFIRMED",
    "transactionRef": null,
    "confirmedAt": "2026-03-15T11:00:00Z",
    "confirmedBy": "admin@idoneo.com"
  },
  "shipping": {
    "method": "SHIPPING",
    "cost": 5000,
    "freeShipping": false,
    "discount": 3100,
    "discountType": "MANUAL_PAYMENT_DISCOUNT"
  },
  "schedule": {
    "isScheduled": false,
    "scheduledDate": null,
    "scheduledTimeSlot": null
  },
  "subtotal": 62000,
  "shippingCost": 5000,
  "discount": 3100,
  "totalAmount": 63900,
  "status": "CONFIRMED",
  "statusHistory": [
    { "status": "PENDING", "timestamp": "2026-03-15T10:30:00Z", "note": null },
    { "status": "CONFIRMED", "timestamp": "2026-03-15T11:00:00Z", "note": "Pago recibido por transferencia" }
  ],
  "notes": [
    { "id": "note-001", "authorEmail": "admin@idoneo.com", "content": "Cliente frecuente", "createdAt": "2026-03-15T11:05:00Z" }
  ],
  "createdAt": "2026-03-15T10:30:00Z",
  "updatedAt": "2026-03-15T11:00:00Z"
}
```

### `PUT /api/v1/admin/orders/:code/status`

Actualizar estado del pedido.

**Auth**: Requerida.

**Request body**:
```json
{
  "status": "PREPARING",
  "note": "Pedido en preparación"
}
```

**Validación de transiciones**:
```
PENDING → CONFIRMED
CONFIRMED → PREPARING
PREPARING → SHIPPED
SHIPPED → DELIVERED
ANY (except DELIVERED) → CANCELLED
```

Transiciones inválidas retornan `400 Bad Request`.

### `POST /api/v1/admin/orders/:code/notes`

Agregar nota interna.

**Auth**: Requerida.

**Request body**:
```json
{
  "content": "Cliente llamó para confirmar dirección"
}
```

### `POST /api/v1/admin/orders/manual`

Crear pedido manual (venta presencial/telefónica).

**Auth**: Requerida.

**Request body**:
```json
{
  "customerName": "María López",
  "customerPhone": "+573109876543",
  "customerEmail": "maria@test.com",
  "customerCity": "Barranquilla",
  "customerAddress": "Carrera 50 #72-33",
  "items": [
    { "productId": "prod-001", "quantity": 2 },
    { "productId": "prod-003", "quantity": 1 }
  ],
  "paymentMethod": "CASH",
  "isScheduled": true,
  "scheduledDate": "2026-03-20",
  "scheduledTimeSlot": "MORNING",
  "notes": "Pedido por teléfono"
}
```

**Response** (`201 Created`):
```json
{
  "orderCode": "ORD-2026-0046",
  "status": "CONFIRMED",
  "totalAmount": 62000,
  "message": "Pedido manual creado exitosamente"
}
```

**Implementación**:
- Crea pedido con estado CONFIRMED directamente (no pasa por PENDING).
- Valida que los productos existan y tengan stock.
- Descuenta stock.
- Crea/actualiza registro del cliente (F040).
- Envía email de confirmación al cliente si tiene email.

### `GET /api/v1/admin/orders/export`

Exportar pedidos filtrados a CSV.

**Auth**: Requerida.

**Query params**: Mismos filtros que `GET /admin/orders`.

**Response**: `Content-Type: text/csv` con el archivo CSV.

**Columnas CSV**: Código, Fecha, Cliente, Email, Teléfono, Ciudad, Items, Subtotal, Envío, Descuento, Total, Estado, Método de Pago.

## Arquitectura (Hexagonal)

```
infrastructure/
├── web/
│   └── AdminOrderController.java
├── persistence/
│   └── DynamoAdminOrderRepository.java
application/
├── usecase/
│   ├── ListAdminOrdersUseCase.java
│   ├── GetAdminOrderDetailUseCase.java
│   ├── UpdateOrderStatusUseCase.java
│   ├── AddOrderNoteUseCase.java
│   ├── CreateManualOrderUseCase.java
│   └── ExportOrdersUseCase.java
domain/
├── model/
│   ├── Order.java              // (se extiende)
│   ├── OrderNote.java          // Nuevo
│   └── OrderStatusTransition.java // Reglas de transición
├── port/
│   └── OrderRepository.java    // (se extiende)
```

## Comentarios en código

Sí, todos los archivos Java deben tener comentarios explicatorios en español.

## Notas

- Las transiciones de estado son estrictas — no se puede saltar pasos ni ir hacia atrás.
- Al cambiar estado, se actualiza el statusHistory del pedido.
- Al cambiar estado, se dispara email de notificación al comprador (F043).
- Al cancelar, se restaura el stock de los productos.
- Los pedidos manuales generan automáticamente un código de orden secuencial.
- El export CSV maneja correctamente caracteres especiales (UTF-8 BOM para Excel).
