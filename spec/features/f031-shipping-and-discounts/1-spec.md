# F031 — Shipping Cost, Free Shipping & Manual Payment Discount (Backend)

## Propósito

Implementar la lógica de cálculo de costos de envío, envío gratis condicional y descuento por pago manual en el backend. Estos cálculos se ejecutan al crear la orden y se validan contra la configuración del tenant.

## Alcance

### Entidades de Dominio

- `ShippingConfig` — Sub-entidad de TenantConfig. Configuración de envío por tenant.
- `ManualPaymentDiscountConfig` — Sub-entidad de TenantConfig. Configuración de descuento por pago manual.
- Campos nuevos en `Order`: `manualPaymentDiscount`, `manualPaymentDiscountRate`, `freeShippingApplied`.

### Use Cases

- `CalculateOrderTotalsUseCase` (nuevo) — Calcula subtotal, shipping, descuento y total. Usado por `CreateOrderUseCase`.

### Modificaciones a Use Cases existentes

- `CreateOrderUseCase` (F005) — Integra `CalculateOrderTotalsUseCase` para calcular totales antes de persistir la orden.

## CalculateOrderTotalsUseCase — Lógica

```
Entrada:
  - items: List<OrderItem> (con precios ya validados contra DB)
  - deliveryMethod: SHIPPING | PICKUP
  - paymentMethod: WOMPI | BANK_TRANSFER | CASH_ON_DELIVERY
  - tenantConfig: TenantConfig (con shippingConfig y manualPaymentDiscount)

Proceso:
1. subtotal = Σ(item.price × item.quantity) para todos los items
2. Calcular shippingCost:
   a. Si deliveryMethod == PICKUP → shippingCost = 0
   b. Si shippingConfig == null → shippingCost = 0 (backward compatible)
   c. Si shippingConfig.freeShippingThreshold != null AND subtotal >= freeShippingThreshold
      → shippingCost = 0, freeShippingApplied = true
   d. Else → shippingCost = shippingConfig.defaultShippingCost
3. Calcular manualPaymentDiscount:
   a. Si manualPaymentDiscount == null OR enabled == false → discount = 0
   b. Si paymentMethod NO está en applicableMethods → discount = 0
   c. Else → discount = Math.round(subtotal × discountRate)
4. total = subtotal - discount + shippingCost
5. Validar total > 0

Salida:
  - subtotal, shippingCost, freeShippingApplied, manualPaymentDiscount, manualPaymentDiscountRate, total
```

### Validaciones en CreateOrderUseCase

```
1. Si tenantConfig.minimumOrderAmount != null AND subtotal < minimumOrderAmount:
   → 400 ORDER_BELOW_MINIMUM "El pedido mínimo es de $X"

2. Si paymentMethod == WOMPI AND discount > 0:
   → 400 INVALID_DISCOUNT "Descuento no aplica para pagos con Wompi"

3. Si deliveryMethod == PICKUP AND shippingConfig.pickupEnabled == false:
   → 400 PICKUP_NOT_AVAILABLE "Recogida en tienda no disponible"

4. Recalcular totales en backend (NUNCA confiar en datos del frontend)
```

## Endpoints

### POST `/api/v1/tenants/:tenantId/orders` (modifica F005)

**Request Body** — sin cambios en la estructura, los campos `deliveryMethod` y `paymentMethod` ya existen.

**Response 201** — campos nuevos:

```json
{
  "orderId": "uuid",
  "orderCode": "ORD-ABCD-1234",
  "tenantId": "chicha",
  "customer": { "..." },
  "items": [ "..." ],
  "deliveryMethod": "SHIPPING",
  "paymentMethod": "BANK_TRANSFER",
  "subtotal": 40000,
  "shippingCost": 0,
  "freeShippingApplied": true,
  "manualPaymentDiscount": 2000,
  "manualPaymentDiscountRate": 0.05,
  "total": 38000,
  "paymentStatus": "PENDING",
  "orderStatus": "PENDING",
  "createdAt": "2026-03-14T23:00:00Z"
}
```

### GET `/api/v1/tenants/:tenantId/orders/:orderCode` (modifica F005)

Response incluye los mismos campos nuevos para que el tracking y thank-you page los muestren.

### GET `/api/v1/tenants/:tenantId/checkout/calculate` (nuevo, opcional)

Endpoint para que el frontend pueda pre-calcular totales antes de crear la orden. Útil para validar sin persistir.

**Query params**: `subtotal`, `deliveryMethod`, `paymentMethod`

**Response 200:**
```json
{
  "subtotal": 40000,
  "shippingCost": 0,
  "freeShippingApplied": true,
  "manualPaymentDiscount": 2000,
  "manualPaymentDiscountRate": 0.05,
  "total": 38000,
  "savings": 7000,
  "savingsBreakdown": {
    "freeShipping": 5000,
    "manualPaymentDiscount": 2000
  }
}
```

## Integración con F030 (Commission Calculation)

El `CalculateCommissionUseCase` (F030) usa el **total de la orden** (después de descuento y envío) como `grossAmount`:

```
grossAmount = order.total (= subtotal - manualPaymentDiscount + shippingCost)
```

Esto significa que:
- El descuento por pago manual reduce ligeramente la base de comisión.
- Pero como F030 demostró, la plataforma GANA MÁS en neto porque no paga el fee de Wompi.
- El envío NO afecta la comisión significativamente (es un monto pequeño).

## Manejo de Errores

| Código | Escenario | Respuesta |
|--------|-----------|-----------|
| `400` | Subtotal menor al pedido mínimo del tenant | `ORDER_BELOW_MINIMUM` + mínimo requerido |
| `400` | Descuento manual aplicado con pago Wompi | `INVALID_DISCOUNT` |
| `400` | Recogida en tienda no disponible | `PICKUP_NOT_AVAILABLE` |
| `400` | Total calculado <= 0 | `INVALID_ORDER_TOTAL` |

## DynamoDB

No requiere cambios en access patterns. Los campos nuevos se almacenan dentro del registro de Order existente (PK: `TENANT#<tenantId>`, SK: `ORDER#<orderId>`).

Campos adicionales en el Map de la orden:
- `shippingCost` (Number)
- `freeShippingApplied` (Boolean)
- `manualPaymentDiscount` (Number)
- `manualPaymentDiscountRate` (Number)

## Notas

- Comentarios en el código en español (regla del proyecto).
- `CalculateOrderTotalsUseCase` es un use case puro (sin efectos secundarios, sin I/O). Solo recibe datos y retorna el cálculo. Esto facilita testing.
- El backend SIEMPRE recalcula. Si el frontend envía un total diferente al calculado, se ignora y se usa el del backend.
- Para backward compatibility: si un tenant no tiene `shippingConfig`, el envío es $0. Si no tiene `manualPaymentDiscount`, no se aplica descuento.
- El endpoint GET `/checkout/calculate` es opcional para MVP. El frontend puede calcular localmente. Pero es útil para validación pre-submit.
