# F038 — Sales Reports & Analytics (Backend Spec)

## Endpoints

### `GET /api/v1/admin/reports/sales`

Reporte de ventas con métricas y datos para gráficos.

**Auth**: Requerida (MERCHANT, SUPER_ADMIN).

**Query params**:
| Param | Tipo | Descripción |
|-------|------|-------------|
| `tenantId` | String | Solo SUPER_ADMIN; MERCHANT auto-filtra |
| `from` | String | Fecha inicio ISO 8601 |
| `to` | String | Fecha fin ISO 8601 |

**Response** (`200 OK`):
```json
{
  "totalRevenue": 2500000,
  "totalOrders": 45,
  "avgTicket": 55556,
  "platformCommission": 375000,
  "conversionRate": 87.5,
  "revenueByDay": [
    { "date": "2026-03-01", "revenue": 150000, "prevRevenue": 120000, "orders": 3 }
  ],
  "ordersByStatus": [
    { "status": "DELIVERED", "count": 35, "percentage": 77.8 },
    { "status": "CANCELLED", "count": 5, "percentage": 11.1 },
    { "status": "PENDING", "count": 5, "percentage": 11.1 }
  ],
  "ordersByPaymentMethod": [
    { "method": "WOMPI", "count": 20, "amount": 1200000 },
    { "method": "BANK_TRANSFER", "count": 15, "amount": 800000 },
    { "method": "CASH", "count": 10, "amount": 500000 }
  ]
}
```

**Cálculos**:
- `platformCommission` = totalRevenue × 0.15
- `conversionRate` = (pedidos DELIVERED / total pedidos) × 100
- `prevRevenue` = ingresos del mismo día pero en el periodo anterior equivalente

### `GET /api/v1/admin/reports/products`

Top productos vendidos en el rango.

**Auth**: Requerida.

**Query params**: `tenantId`, `from`, `to`, `limit` (default: 10).

**Response** (`200 OK`):
```json
[
  {
    "productId": "prod-001",
    "name": "Bowl Energético",
    "imageUrl": "https://cdn.../bowl.jpg",
    "quantitySold": 25,
    "revenue": 625000,
    "changePercent": 15.3
  }
]
```

### `GET /api/v1/admin/reports/customers`

Métricas de clientes en el rango.

**Auth**: Requerida.

**Response** (`200 OK`):
```json
{
  "totalCustomers": 38,
  "newCustomers": 12,
  "returningCustomers": 26,
  "topCities": [
    { "city": "Barranquilla", "count": 20 },
    { "city": "Bogotá", "count": 10 },
    { "city": "Medellín", "count": 8 }
  ]
}
```

### `GET /api/v1/admin/reports/stores`

Resumen por tienda (solo SUPER_ADMIN).

**Auth**: Requerida (SUPER_ADMIN only).

**Response** (`200 OK`):
```json
[
  {
    "tenantId": "idoneo",
    "businessName": "IDONEO",
    "logoUrl": "https://cdn.../idoneo/logo.png",
    "revenue": 1200000,
    "orders": 22,
    "avgTicket": 54545,
    "commission": 180000
  }
]
```

### `GET /api/v1/admin/reports/export`

Exportar reporte completo a CSV.

**Auth**: Requerida.

**Query params**: `tenantId`, `from`, `to`, `type` (`sales`, `products`, `customers`).

**Response**: `Content-Type: text/csv`.

## Arquitectura (Hexagonal)

```
infrastructure/
├── web/
│   └── AdminReportController.java
├── persistence/
│   └── DynamoReportRepository.java
application/
├── usecase/
│   ├── GetSalesReportUseCase.java
│   ├── GetTopProductsReportUseCase.java
│   ├── GetCustomerMetricsUseCase.java
│   ├── GetStoresSummaryUseCase.java
│   └── ExportReportUseCase.java
domain/
├── model/
│   ├── SalesReport.java
│   ├── TopProductReport.java
│   ├── CustomerMetrics.java
│   └── StoreSummary.java
├── port/
│   └── ReportRepository.java
```

## Optimización de Queries

- Usar GSI con SK que incluya fecha para queries eficientes por rango.
- Para top products: iterar sobre items de órdenes y agregar en memoria.
- Cache de reportes con TTL 15 min para rangos ya cerrados (mes pasado, etc.).
- Para rangos muy grandes (1 año), considerar agrupar por semana/mes.

## Comentarios en código

Sí, todos los archivos Java deben tener comentarios explicatorios en español.

## Notas

- La comisión del 15% es un cálculo informativo — la dispersión real es F030.
- `conversionRate` cuenta solo pedidos con estado final (DELIVERED o CANCELLED).
- El periodo anterior para comparativa se calcula automáticamente: si el rango es 30 días, el anterior son los 30 días previos.
- Los reportes por tienda (stores) solo están disponibles para SUPER_ADMIN.
- Los exports CSV incluyen BOM UTF-8 para compatibilidad con Excel.
