# F040 — Customer Database & CRM (Backend Spec)

## Endpoints

### `GET /api/v1/admin/customers`

Listar clientes del tenant con paginación y filtros.

**Auth**: Requerida (MERCHANT, SUPER_ADMIN).

**Query params**:
| Param | Tipo | Descripción |
|-------|------|-------------|
| `tenantId` | String | Solo SUPER_ADMIN |
| `search` | String | Buscar por nombre, email o teléfono |
| `city` | String | Filtrar por ciudad |
| `minSpent` | Number | Mínimo total gastado |
| `maxSpent` | Number | Máximo total gastado |
| `page` | Number | Página (default: 1) |
| `limit` | Number | Items por página (default: 20) |
| `sort` | String | `spent_desc`, `spent_asc`, `name_asc`, `last_order_desc` |

**Response** (`200 OK`):
```json
{
  "items": [
    {
      "id": "cust-001",
      "name": "Juan Pérez",
      "email": "juan@test.com",
      "phone": "+573001234567",
      "city": "Barranquilla",
      "totalOrders": 5,
      "totalSpent": 320000,
      "avgTicket": 64000,
      "lastOrderDate": "2026-03-10T14:30:00Z"
    }
  ],
  "total": 38,
  "page": 1,
  "limit": 20,
  "totalPages": 2
}
```

### `GET /api/v1/admin/customers/:id`

Perfil completo del cliente.

**Auth**: Requerida.

**Response** (`200 OK`):
```json
{
  "id": "cust-001",
  "name": "Juan Pérez",
  "email": "juan@test.com",
  "phone": "+573001234567",
  "city": "Barranquilla",
  "address": "Calle 123 #45-67",
  "totalOrders": 5,
  "totalSpent": 320000,
  "avgTicket": 64000,
  "firstOrderDate": "2026-01-15T10:00:00Z",
  "lastOrderDate": "2026-03-10T14:30:00Z",
  "favoriteProducts": [
    { "productId": "prod-001", "name": "Bowl Energético", "imageUrl": "https://cdn.../bowl.jpg", "count": 4 },
    { "productId": "prod-003", "name": "Jugo Verde", "imageUrl": "https://cdn.../jugo.jpg", "count": 3 }
  ]
}
```

### `GET /api/v1/admin/customers/:id/orders`

Historial de pedidos del cliente.

**Auth**: Requerida.

**Response** (`200 OK`):
```json
[
  {
    "orderCode": "ORD-2026-0045",
    "createdAt": "2026-03-10T14:30:00Z",
    "totalAmount": 63900,
    "status": "DELIVERED",
    "itemCount": 3
  }
]
```

### `GET /api/v1/admin/customers/export`

Exportar lista de clientes a CSV.

**Auth**: Requerida.

**Response**: `Content-Type: text/csv`.

**Columnas**: Nombre, Email, Teléfono, Ciudad, Total Pedidos, Total Gastado, Ticket Promedio, Primera Compra, Última Compra.

## Servicio de Upsert de Clientes

### `UpsertCustomerService`

Se ejecuta automáticamente al crear un pedido (F005, F037).

**Lógica**:
1. Buscar cliente existente por email (primary) o teléfono (secondary) para el tenant.
2. Si existe → actualizar: nombre, dirección, ciudad (últimos datos), incrementar totalOrders, sumar al totalSpent, actualizar lastOrderDate, recalcular avgTicket.
3. Si no existe → crear nuevo registro con datos del checkout.
4. Actualizar índices de búsqueda (email, teléfono).

**Deduplicación**:
- Primary key de deduplicación: email dentro del mismo tenant.
- Secondary: teléfono (si el email es diferente pero el teléfono coincide, se trata como el mismo cliente).

## Arquitectura (Hexagonal)

```
infrastructure/
├── web/
│   └── AdminCustomerController.java
├── persistence/
│   └── DynamoCustomerRepository.java
application/
├── usecase/
│   ├── ListCustomersUseCase.java
│   ├── GetCustomerProfileUseCase.java
│   ├── GetCustomerOrdersUseCase.java
│   ├── UpsertCustomerUseCase.java
│   └── ExportCustomersUseCase.java
domain/
├── model/
│   ├── Customer.java
│   └── CustomerFavoriteProduct.java
├── port/
│   └── CustomerRepository.java
```

## DynamoDB

| PK | SK | Datos |
|----|-----|-------|
| `TENANT#<tenantId>` | `CUSTOMER#<id>` | name, email, phone, city, address, totalOrders, totalSpent, avgTicket, firstOrderDate, lastOrderDate |
| `TENANT#<tenantId>` | `CUSTOMER_EMAIL#<email>` | customerId |
| `TENANT#<tenantId>` | `CUSTOMER_PHONE#<phone>` | customerId |

### Cálculo de productos favoritos

- Al consultar el perfil, se obtienen todos los pedidos del cliente.
- Se iteran los items de cada pedido y se cuentan por productId.
- Se retornan los top 5 más comprados.
- Considerar cache o pre-cálculo si el volumen crece.

## Comentarios en código

Sí, todos los archivos Java deben tener comentarios explicatorios en español.

## Notas

- Los clientes son entidades pasivas — se crean automáticamente al recibir pedidos.
- No hay endpoint de creación manual de clientes.
- La deduplicación por email es estricta (case-insensitive, trimmed).
- La búsqueda (`search`) es case-insensitive y busca en nombre, email y teléfono.
- MERCHANT no puede acceder a clientes de otros tenants — el filtro es automático.
- No se almacenan datos sensibles de pago en el registro del cliente.
