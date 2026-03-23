# F006 — Wompi Integration (Backend)

## Propósito

Integrar Wompi para pagos en línea: generar datos de inicialización del widget, procesar webhooks de confirmación y verificar transacciones.

## Endpoints

| Método | Ruta                                                         | Descripción               | Auth                        |
| ------ | ------------------------------------------------------------ | ------------------------- | --------------------------- |
| `POST` | `/api/v1/tenants/:tenantId/payments/wompi/init`              | Generar datos para widget | No                          |
| `POST` | `/api/v1/webhooks/wompi`                                     | Webhook de Wompi          | No (verificación por firma) |
| `GET`  | `/api/v1/tenants/:tenantId/orders/:orderCode/payment-status` | Estado del pago           | No                          |

## Domain Layer

### Ports

**PaymentService**

- `PaymentInitData initiate(String tenantId, Order order)` — Genera referencia y firma
- `boolean verifyWebhookSignature(String payload, String signature)` — Verifica firma del webhook
- `TransactionStatus getTransactionStatus(String transactionId)` — Consulta estado en Wompi API

### Use Cases

| Interface                     | Input                               | Output                  |
| ----------------------------- | ----------------------------------- | ----------------------- |
| `InitiateWompiPaymentUseCase` | `String tenantId, String orderCode` | `PaymentInitData`       |
| `ProcessWompiWebhookUseCase`  | `WompiWebhookEvent`                 | `void`                  |
| `GetPaymentStatusUseCase`     | `String tenantId, String orderCode` | `PaymentStatusResponse` |

## Application Layer

### InitiateWompiPaymentUseCaseImpl

1. Buscar orden por orderCode.
2. Validar que paymentMethod = WOMPI.
3. Generar referencia de pago: `PX-<tenantId>-<orderCode>`.
4. Calcular firma de integridad: `SHA256(referencia + monto_centavos + "COP" + integrity_secret)`.
5. Retornar: referencia, monto en centavos, moneda, firma, publicKey.

### ProcessWompiWebhookUseCaseImpl

1. Verificar firma del webhook con events_secret.
2. Extraer transactionId y status del evento.
3. Extraer orderCode de la referencia.
4. Buscar la orden.
5. Si status == APPROVED:
   - Actualizar paymentStatus → PAID
   - Actualizar orderStatus → CONFIRMED
   - Descontar stock
   - Agregar StatusHistory
   - Disparar notificación email (F009)
6. Si status == DECLINED/VOIDED/ERROR:
   - Actualizar paymentStatus → FAILED
   - Agregar StatusHistory
7. **Idempotencia**: si la orden ya está PAID, ignorar el webhook duplicado.

## Wompi API

- Sandbox: `https://sandbox.wompi.co/v1/`
- Production: `https://production.wompi.co/v1/`
- Firma de integridad: `SHA256(reference + amount_in_cents + currency + integrity_secret)`
- Webhook events: `transaction.updated`

## Criterios de Aceptación

- [x] `POST /payments/wompi/init` retorna referencia, firma y datos para widget.
- [x] `POST /webhooks/wompi` procesa evento y actualiza orden.
- [x] Firma inválida en webhook retorna `401`.
- [x] Pago aprobado actualiza orden a CONFIRMED y PAID.
- [x] Pago rechazado actualiza paymentStatus a FAILED.
- [x] Webhook duplicado no procesa dos veces (idempotencia).
- [x] `GET /payment-status` retorna estado actual.

## Testing

### Unitarios

- `InitiateWompiPaymentUseCaseImplTest` — inicio exitoso, orden no encontrada, método no Wompi, orden ya pagada
- `ProcessWompiWebhookUseCaseImplTest` — pago aprobado, pago rechazado, firma inválida, idempotencia
- `GetPaymentStatusUseCaseImplTest` — estado encontrado, orden no encontrada

## Notas

- El monto se envía en centavos: $50,000 COP = 5000000.
- NUNCA almacenar datos de tarjeta.
- El webhook URL debe ser accesible desde internet (en desarrollo usar ngrok o similar).
- El events_secret se obtiene del dashboard de Wompi y se guarda en Secrets Manager.
