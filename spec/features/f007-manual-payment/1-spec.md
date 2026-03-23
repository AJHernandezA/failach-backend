# F007 — Manual Payment API (Backend)

## Propósito

Implementar la confirmación de pagos manuales (transferencia bancaria y efectivo contraentrega) y la cancelación automática de órdenes no confirmadas.

## Endpoints

| Método | Ruta                                                          | Descripción           | Auth                   |
| ------ | ------------------------------------------------------------- | --------------------- | ---------------------- |
| `PUT`  | `/api/v1/tenants/:tenantId/orders/:orderCode/confirm-payment` | Confirmar pago manual | SUPER_ADMIN / MERCHANT |
| `PUT`  | `/api/v1/tenants/:tenantId/orders/:orderCode/cancel`          | Cancelar orden        | SUPER_ADMIN / MERCHANT |

## Domain Layer

### Use Cases

| Interface                     | Input                                              | Output  |
| ----------------------------- | -------------------------------------------------- | ------- |
| `ConfirmManualPaymentUseCase` | `String tenantId, String orderCode, String note`   | `Order` |
| `CancelOrderUseCase`          | `String tenantId, String orderCode, String reason` | `Order` |

## Application Layer

### ConfirmManualPaymentUseCaseImpl

1. Buscar orden por orderCode.
2. Validar que `paymentMethod` es `BANK_TRANSFER` o `CASH_ON_DELIVERY`.
3. Validar que `paymentStatus` es `PENDING`.
4. Actualizar `paymentStatus` → `PAID`.
5. Actualizar `orderStatus` → `CONFIRMED`.
6. Si es transferencia bancaria: descontar stock ahora (no se descontó al crear).
7. Agregar `StatusHistory` con nota del comercio.
8. Guardar orden.
9. Disparar email de confirmación (F009).
10. Retornar orden actualizada.

### CancelOrderUseCaseImpl

1. Buscar orden por orderCode.
2. Validar que la orden no esté en `DELIVERED` (no se puede cancelar una entregada).
3. Actualizar `orderStatus` → `CANCELLED`.
4. Si el stock ya fue descontado → restaurar stock.
5. Agregar `StatusHistory` con razón de cancelación.
6. Guardar orden.
7. Disparar email de cancelación (F009).
8. Retornar orden actualizada.

## Cancelación Automática (Transferencias no confirmadas)

### Estrategia MVP

- Al crear la orden de transferencia, setear `ttlCancellation` = `createdAt + 24h`.
- Un campo `autoCancelAt` (Instant) en la orden.
- Opción A: DynamoDB TTL + Lambda trigger para cancelar.
- Opción B: Cron en el backend que revisa órdenes pendientes cada hora.
- **MVP**: Opción B (más simple, no requiere Lambda).

### Cron Job

- Cada hora, buscar órdenes con: `paymentMethod = BANK_TRANSFER`, `paymentStatus = PENDING`, `createdAt < now - 24h`.
- Para cada una: ejecutar `CancelOrderUseCase`.
- Loguear cada cancelación automática.

## Criterios de Aceptación

- [x] `PUT /confirm-payment` confirma pago y actualiza orden a CONFIRMED.
- [x] Solo SUPER_ADMIN o MERCHANT pueden confirmar.
- [x] Confirmar orden ya pagada retorna `422` "Orden ya está pagada".
- [x] `PUT /cancel` cancela orden y restaura stock si fue descontado.
- [x] No se puede cancelar orden DELIVERED.
- [ ] Transferencia no confirmada en 24h se cancela automáticamente (deuda técnica MVP: cron).

## Testing

### Unitarios

- `ConfirmManualPaymentUseCaseImplTest` — confirmación exitosa, ya pagada, método inválido
- `CancelOrderUseCaseImplTest` — cancelación exitosa, restaurar stock, orden entregada

## Notas

- En MVP, el cron de cancelación automática puede ser un `ScheduledExecutorService` en Java.
- A futuro, migrar a un EventBridge rule + Lambda para mayor confiabilidad.
