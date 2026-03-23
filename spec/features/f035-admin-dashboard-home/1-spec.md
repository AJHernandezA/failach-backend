# F035 — Admin Dashboard Home (Backend Spec)

## Endpoints

### `GET /api/v1/admin/dashboard`

Retorna métricas agregadas para el dashboard principal.

**Auth**: Requerida (SUPER_ADMIN o MERCHANT).

**Query params**:
| Param | Tipo | Requerido | Descripción |
|-------|------|-----------|-------------|
| `tenantId` | String | No | Filtrar por tienda (solo SUPER_ADMIN; MERCHANT auto-filtra) |
| `from` | String | No | Fecha inicio ISO 8601 (default: hace 30 días) |
| `to` | String | No | Fecha fin ISO 8601 (default: hoy) |

**Response** (`200 OK`):
```json
{
  "totalRevenue": 2500000,
  "totalOrders": 45,
  "totalProducts": 28,
  "avgTicket": 55556,
  "revenueChange": 15.3,
  "ordersChange": 8.2,
  "salesByDay": [
    { "date": "2026-03-01", "revenue": 150000, "orders": 3 },
    { "date": "2026-03-02", "revenue": 200000, "orders": 5 }
  ],
  "recentOrders": [
    {
      "orderCode": "ORD-2026-0045",
      "customerName": "Juan Pérez",
      "amount": 85000,
      "status": "CONFIRMED",
      "paymentMethod": "BANK_TRANSFER",
      "createdAt": "2026-03-15T10:30:00Z"
    }
  ],
  "topProducts": [
    {
      "productId": "prod-001",
      "name": "Bowl Energético",
      "imageUrl": "https://cdn.../bowl.jpg",
      "quantitySold": 25,
      "revenue": 625000
    }
  ]
}
```

**Implementación**:
- `AdminDashboardController.getDashboard()` → `GetDashboardMetricsUseCase`
- Si MERCHANT → auto-filtra por su tenantId.
- Si SUPER_ADMIN sin tenantId → consolida todas las tiendas.
- Si SUPER_ADMIN con tenantId → filtra por esa tienda.

## Arquitectura (Hexagonal)

```
infrastructure/
├── web/
│   └── AdminDashboardController.java
application/
├── usecase/
│   └── GetDashboardMetricsUseCase.java
domain/
├── model/
│   ├── DashboardMetrics.java
│   ├── SalesByDay.java
│   └── TopProduct.java
├── port/
│   ├── OrderRepository.java         // (ya existe, se extiende)
│   └── ProductRepository.java       // (ya existe, se extiende)
```

## Queries DynamoDB

### Total revenue y orders en rango
- Scan/Query de órdenes por tenant + fecha en rango.
- Filtrar por status != CANCELLED.
- Sumar amounts para totalRevenue.
- Contar para totalOrders.

### Revenue change (vs periodo anterior)
- Calcular el rango anterior equivalente (ej: si rango = 30 días, el anterior son los 30 días previos).
- Comparar totalRevenue actual vs anterior → % change.

### Sales by day
- Agrupar órdenes por fecha (truncar timestamp a día).
- Sumar revenue y contar por día.

### Recent orders
- Query últimas 10 órdenes por fecha desc.

### Top products
- Agrupar items de todas las órdenes por productId.
- Sumar quantities y revenues.
- Ordenar por quantitySold desc, tomar top 5.

## Optimización

- Cachear resultado en memoria (TTL 5 min, key = tenantId+from+to).
- Las queries de "periodo anterior" se pueden cachear más agresivamente.
- Para volúmenes grandes, considerar pre-agregaciones con DynamoDB Streams.

## Comentarios en código

Sí, todos los archivos Java deben tener comentarios explicatorios en español.

## Notas

- MERCHANT no puede pasar `tenantId` de otro tenant — el filtro es automático.
- Si SUPER_ADMIN no pasa `tenantId`, consolida TODAS las tiendas.
- Los montos son en centavos (COP sin decimales).
- El `avgTicket` se calcula como totalRevenue / totalOrders (0 si no hay pedidos).
- `revenueChange` y `ordersChange` son porcentajes (positivo = crecimiento, negativo = decrecimiento).
