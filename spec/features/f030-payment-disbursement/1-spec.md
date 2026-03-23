# F030 — Payment Disbursement & Commission (Backend)

## Propósito

Implementar la lógica de negocio para calcular comisiones de la plataforma (tasa configurable por tenant), registrarlas, y ejecutar dispersiones de pagos hacia las cuentas bancarias de los comercios (tenants) usando la API de Wompi Payouts. Soporta modelo PERCENTAGE (tasa fija) y TIERED (escalonado por monto).

## Alcance

### Entidades de Dominio

- `Commission` — Registro de comisión calculada por cada orden pagada.
- `Disbursement` — Registro de una dispersión ejecutada hacia un tenant.
- `DisbursementBatch` — Agrupación de comisiones para dispersión.
- `TenantBankInfo` — Datos bancarios del tenant (sub-entidad de TenantConfig de F002).

### Use Cases

- `CalculateCommissionUseCase` — Se ejecuta automáticamente al confirmar pago. Calcula y registra la comisión.
- `CreateDisbursementUseCase` — Agrupa comisiones pendientes de un tenant y crea un registro de dispersión.
- `ExecuteDisbursementUseCase` — Envía la dispersión a Wompi Payouts API.
- `ProcessPayoutsWebhookUseCase` — Procesa webhook de Wompi Payouts y actualiza estado.
- `GetTenantBalanceUseCase` — Calcula balance pendiente de un tenant.
- `GetTenantFinanceSummaryUseCase` — Resumen financiero de un tenant.
- `GetGlobalFinanceSummaryUseCase` — Resumen financiero global (SUPER_ADMIN).
- `ListCommissionsUseCase` — Lista comisiones con filtros.
- `ListDisbursementsUseCase` — Lista dispersiones con filtros.
- `GetDisbursementDetailUseCase` — Detalle de una dispersión.
- `UpdateTenantBankInfoUseCase` — Actualizar datos bancarios del tenant.
- `GetTenantBankInfoUseCase` — Obtener datos bancarios del tenant.

### Servicios

- `WompiPayoutsService` — Cliente HTTP para Wompi Payouts API.
  - `authenticate()` — Obtener token de autenticación.
  - `getBanks()` — Lista de bancos soportados.
  - `getAccounts()` — Cuentas origen configuradas.
  - `createPayoutBatch(batch)` — Crear lote de pago.
  - `getBatchStatus(batchId)` — Consultar estado de lote.
  - `getTransactionStatus(transactionId)` — Consultar estado de transacción.
  - `verifyWebhookSignature(payload, signature)` — Verificar firma de webhook.
- `CommissionCalculatorService` — Lógica pura de cálculo de comisiones. Soporta modelos PERCENTAGE y TIERED. Lee config de `TenantConfig.commissionConfig`.
- `DisbursementSchedulerService` — Cron job para dispersiones automáticas.

### Repositories (DynamoDB)

- `CommissionRepository`
  - `save(commission)` — Guardar comisión.
  - `findByTenantId(tenantId, filters)` — Listar comisiones de un tenant.
  - `findPendingByTenantId(tenantId)` — Comisiones con status CALCULATED.
  - `updateStatus(commissionId, status, disbursementId)` — Marcar como DISBURSED.
  - `sumPendingByTenantId(tenantId)` — Suma de merchantAmount pendiente.
- `DisbursementRepository`
  - `save(disbursement)` — Guardar dispersión.
  - `findByTenantId(tenantId, filters)` — Listar dispersiones de un tenant.
  - `findById(tenantId, disbursementId)` — Obtener dispersión.
  - `findByWompiBatchId(batchId)` — Buscar por ID de lote Wompi.
  - `updateStatus(disbursementId, status, details)` — Actualizar estado.
  - `findAll(filters)` — Listar todas (cross-tenant, SUPER_ADMIN).

## Endpoints

| Método | Ruta                                          | Descripción                   | Auth                   |
| ------ | --------------------------------------------- | ----------------------------- | ---------------------- |
| `PUT`  | `/api/v1/tenants/:tenantId/bank-info`         | Configurar datos bancarios    | SUPER_ADMIN / MERCHANT |
| `GET`  | `/api/v1/tenants/:tenantId/bank-info`         | Obtener datos bancarios       | SUPER_ADMIN / MERCHANT |
| `GET`  | `/api/v1/tenants/:tenantId/balance`           | Balance pendiente             | SUPER_ADMIN / MERCHANT |
| `GET`  | `/api/v1/tenants/:tenantId/finance/summary`   | Resumen financiero tenant     | SUPER_ADMIN / MERCHANT |
| `GET`  | `/api/v1/tenants/:tenantId/commissions`       | Listar comisiones             | SUPER_ADMIN / MERCHANT |
| `GET`  | `/api/v1/tenants/:tenantId/disbursements`     | Listar dispersiones tenant    | SUPER_ADMIN / MERCHANT |
| `GET`  | `/api/v1/tenants/:tenantId/disbursements/:id` | Detalle dispersión            | SUPER_ADMIN / MERCHANT |
| `POST` | `/api/v1/admin/disbursements/execute`         | Ejecutar dispersión manual    | SUPER_ADMIN            |
| `POST` | `/api/v1/webhooks/wompi-payouts`              | Webhook Wompi Payouts         | Público (firma)        |
| `GET`  | `/api/v1/admin/disbursements`                 | Listar todas las dispersiones | SUPER_ADMIN            |
| `GET`  | `/api/v1/admin/finance/summary`               | Resumen financiero global     | SUPER_ADMIN            |
| `GET`  | `/api/v1/admin/banks`                         | Lista de bancos (proxy Wompi) | SUPER_ADMIN / MERCHANT |

## Detalle de Endpoints

### PUT `/api/v1/tenants/:tenantId/bank-info`

**Request Body:**

```json
{
  "legalIdType": "NIT",
  "legalId": "900123456",
  "bankId": "uuid-del-banco",
  "bankName": "Bancolombia",
  "accountType": "AHORROS",
  "accountNumber": "12345678901",
  "accountHolderName": "IDONEO SAS",
  "accountHolderEmail": "pagos@idoneo.co"
}
```

**Validaciones:**

- `legalIdType` debe ser CC, NIT o CE.
- `legalId` solo números, 5-20 caracteres.
- `bankId` debe ser un UUID válido que exista en la lista de bancos de Wompi.
- `accountType` debe ser AHORROS o CORRIENTE.
- `accountNumber` solo números, 6-20 caracteres.
- `accountHolderName` 3-100 caracteres.
- `accountHolderEmail` formato email válido.

**Response 200:**

```json
{
  "legalIdType": "NIT",
  "legalId": "900123456",
  "bankId": "uuid-del-banco",
  "bankName": "Bancolombia",
  "accountType": "AHORROS",
  "accountNumber": "****8901",
  "accountHolderName": "IDONEO SAS",
  "accountHolderEmail": "pagos@idoneo.co",
  "verified": false,
  "updatedAt": "2026-03-14T23:00:00Z"
}
```

### GET `/api/v1/tenants/:tenantId/balance`

**Response 200:**

```json
{
  "tenantId": "idoneo",
  "pendingAmount": 245000,
  "pendingCommissionCount": 3,
  "minDisbursementAmount": 50000,
  "canDisburse": true,
  "lastDisbursementAt": "2026-03-10T08:00:00Z"
}
```

### GET `/api/v1/tenants/:tenantId/finance/summary`

**Query params:** `periodStart`, `periodEnd` (ISO dates, opcional, default: mes actual).

**Response 200:**

```json
{
  "tenantId": "idoneo",
  "totalSales": 1500000,
  "totalOrders": 15,
  "totalCommissions": 210000,
  "totalDisbursed": 150000,
  "pendingBalance": 60000,
  "periodStart": "2026-03-01",
  "periodEnd": "2026-03-31"
}
```

### POST `/api/v1/admin/disbursements/execute`

**Request Body:**

```json
{
  "tenantId": "idoneo"
}
```

> Si `tenantId` es null, ejecuta para todos los tenants con balance >= mínimo.

**Response 200:**

```json
{
  "disbursementsCreated": 1,
  "details": [
    {
      "tenantId": "idoneo",
      "disbursementId": "uuid",
      "amount": 245000,
      "commissionsIncluded": 3,
      "status": "PENDING"
    }
  ]
}
```

**Response 400 (sin balance suficiente):**

```json
{
  "error": "INSUFFICIENT_BALANCE",
  "message": "El tenant 'idoneo' tiene un balance de $30,000 que es menor al mínimo de $50,000."
}
```

**Response 400 (sin datos bancarios):**

```json
{
  "error": "MISSING_BANK_INFO",
  "message": "El tenant 'idoneo' no tiene datos bancarios configurados."
}
```

### POST `/api/v1/webhooks/wompi-payouts`

**Request Body (de Wompi):**

```json
{
  "event": "payout_batch.updated",
  "data": {
    "batch_id": "uuid",
    "status": "COMPLETED",
    "transactions": [
      {
        "id": "uuid",
        "reference": "DISB-uuid",
        "status": "COMPLETED",
        "amount": 24500000
      }
    ]
  },
  "signature": {
    "checksum": "sha256-hash",
    "properties": ["batch_id", "status", "timestamp"]
  },
  "timestamp": 1710460800
}
```

**Verificación:**

1. Verificar firma HMAC SHA256 con el webhook secret de Payouts.
2. Buscar Disbursement por `wompiPayoutBatchId`.
3. Actualizar estado según resultado.
4. Si COMPLETED → marcar comisiones como DISBURSED.
5. Si FAILED/REJECTED → registrar motivo, notificar SUPER_ADMIN.

**Response 200:** `{ "ok": true }`

## Integración con F006/F007 (Hooks)

El cálculo de comisiones se integra al flujo existente de pagos:

### En `ProcessWompiWebhookUseCase` (F006):

```
Cuando paymentStatus cambia a PAID:
  → Llamar CalculateCommissionUseCase(order)
```

### En `ConfirmManualPaymentUseCase` (F007):

```
Cuando MERCHANT confirma pago manual:
  → Llamar CalculateCommissionUseCase(order)
```

### CalculateCommissionUseCase — Lógica:

```
1. Recibe: Order (con total, paymentMethod, tenantId)
2. Obtiene TenantConfig.commissionConfig del tenant
3. Calcula wompiRecaudoFee:
   - Si WOMPI: (grossAmount × 0.0265 + 700) × 1.19
   - Si BANK_TRANSFER o CASH_ON_DELIVERY: $0 (no pasa por Wompi)
4. netAmount = grossAmount - wompiRecaudoFee
5. Determina tasa de comisión:
   - Si commissionModel = PERCENTAGE → usa commissionRate (ej: 0.10, 0.15)
   - Si commissionModel = TIERED → busca en commissionTiers el rango que contenga netAmount
   - Si tenant no tiene commissionConfig → usa PLATFORM_DEFAULT_COMMISSION_RATE (env var, default 0.15)
6. platformCommission = netAmount × tasa
7. merchantAmount = netAmount - platformCommission
8. Crea Commission(status: CALCULATED, platformCommissionRate: tasa aplicada)
9. Persiste en DynamoDB
```

### CommissionCalculatorService — Resolución de tasa TIERED:

```
Ejemplo TECH con tiers:
  [{ min: 0, max: 500000, rate: 0.12 },
   { min: 500001, max: 2000000, rate: 0.10 },
   { min: 2000001, max: null, rate: 0.08 }]

Orden de $1,200,000 → cae en tier 2 → rate = 0.10 (10%)
Orden de $3,500,000 → cae en tier 3 → rate = 0.08 (8%)
Orden de $350,000  → cae en tier 1 → rate = 0.12 (12%)
```

## Wompi Payouts API — Integración

### Autenticación

```
POST https://production.wompi.co/v1/payouts/authenticate
Headers:
  X-Api-Key: {WOMPI_PAYOUTS_API_KEY}
  X-Principal-Id: {WOMPI_PAYOUTS_PRINCIPAL_ID}
Response:
  { "token": "Bearer ...", "expires_at": "..." }
```

### Crear Lote de Pago

```
POST https://production.wompi.co/v1/payouts
Headers:
  Authorization: Bearer {token}
  X-Idempotency-Key: {idempotencyKey}
Body:
{
  "account_id": "{WOMPI_PAYOUTS_ACCOUNT_ID}",
  "transactions": [
    {
      "legal_id_type": "NIT",
      "legal_id": "900123456",
      "bank_id": "uuid",
      "account_type": "AHORROS",
      "account_number": "12345678901",
      "name": "IDONEO SAS",
      "email": "pagos@idoneo.co",
      "amount": 24500000,
      "reference": "DISB-{disbursementId}",
      "payment_type": "OTHER"
    }
  ]
}
Response:
{
  "id": "batch-uuid",
  "status": "CREATED",
  "transactions": [...]
}
```

### Consultar Estado

```
GET https://production.wompi.co/v1/payouts/{batchId}
Headers:
  Authorization: Bearer {token}
```

## DynamoDB Keys

### Commission

- **PK**: `TENANT#<tenantId>`
- **SK**: `COMMISSION#<commissionId>`
- **GSI1PK**: `TENANT#<tenantId>#COMMISSION_STATUS`
- **GSI1SK**: `<status>#<createdAt>`
- **GSI2PK**: `ORDER#<orderId>`
- **GSI2SK**: `COMMISSION`

### Disbursement

- **PK**: `TENANT#<tenantId>`
- **SK**: `DISBURSEMENT#<disbursementId>`
- **GSI1PK**: `TENANT#<tenantId>#DISBURSEMENT_STATUS`
- **GSI1SK**: `<status>#<createdAt>`
- **GSI3PK**: `WOMPI_BATCH#<wompiPayoutBatchId>`
- **GSI3SK**: `DISBURSEMENT`

### TenantBankInfo

- Se almacena como parte de la configuración del tenant existente (F002).
- **PK**: `TENANT#<tenantId>`
- **SK**: `CONFIG`
- Campo adicional: `bankInfo` (Map con los datos bancarios).

## Cron Job — DisbursementSchedulerService

```
Frecuencia: Configurable (default: semanal, lunes 8am COT)

Flujo:
1. Obtener lista de tenants activos.
2. Para cada tenant:
   a. Verificar que tenga bankInfo configurado y verified = true.
   b. Obtener suma de comisiones pendientes (CALCULATED).
   c. Si suma >= minDisbursementAmount:
      - Crear Disbursement (agrupa las comisiones).
      - Ejecutar vía WompiPayoutsService.
   d. Si suma < mínimo → skip, log info.
3. Registrar resultado en logs.
4. Si hay fallos, enviar email a SUPER_ADMIN (vía F009 SES).
```

**Implementación en Javalin:**

- Usar `ScheduledExecutorService` de Java con `scheduleAtFixedRate`.
- O un `Timer` simple que se ejecuta en el intervalo configurado.
- Se inicia al arrancar la aplicación en `Main.java`.

## Guice Module

```java
// DisbursementModule.java — registra todos los componentes de F030
public class DisbursementModule extends AbstractModule {
    @Override
    protected void configure() {
        // Use cases
        bind(CalculateCommissionUseCase.class);
        bind(CreateDisbursementUseCase.class);
        bind(ExecuteDisbursementUseCase.class);
        bind(ProcessPayoutsWebhookUseCase.class);
        bind(GetTenantBalanceUseCase.class);
        bind(GetTenantFinanceSummaryUseCase.class);
        bind(GetGlobalFinanceSummaryUseCase.class);
        bind(ListCommissionsUseCase.class);
        bind(ListDisbursementsUseCase.class);
        bind(GetDisbursementDetailUseCase.class);
        bind(UpdateTenantBankInfoUseCase.class);
        bind(GetTenantBankInfoUseCase.class);

        // Servicios
        bind(WompiPayoutsService.class);
        bind(CommissionCalculatorService.class);
        bind(DisbursementSchedulerService.class);

        // Repositories
        bind(CommissionRepository.class);
        bind(DisbursementRepository.class);
    }
}
```

## Manejo de Errores

| Código | Escenario                                   | Respuesta                                        |
| ------ | ------------------------------------------- | ------------------------------------------------ |
| `400`  | Datos bancarios inválidos                   | `INVALID_BANK_INFO` + detalle de validación      |
| `400`  | Balance insuficiente para dispersión        | `INSUFFICIENT_BALANCE` + balance actual y mínimo |
| `400`  | Sin datos bancarios configurados            | `MISSING_BANK_INFO`                              |
| `404`  | Dispersión no encontrada                    | `DISBURSEMENT_NOT_FOUND`                         |
| `409`  | Dispersión ya en proceso para este tenant   | `DISBURSEMENT_IN_PROGRESS`                       |
| `422`  | Error al crear lote en Wompi Payouts        | `WOMPI_PAYOUTS_ERROR` + detalle                  |
| `500`  | Error de comunicación con Wompi Payouts API | `EXTERNAL_SERVICE_ERROR`                         |

## Seguridad

- `accountNumber` se almacena encriptado con AWS KMS (campo `encryptedAccountNumber` en DynamoDB).
- En responses API, `accountNumber` se enmascara: `****1234` (últimos 4 dígitos).
- Solo se muestra completo en el response del PUT (al configurar).
- Webhook de Payouts verifica firma HMAC SHA256 antes de procesar.
- Todos los endpoints financieros requieren `SUPER_ADMIN` o `MERCHANT` del tenant.
- Logs de auditoría (sin datos sensibles) para toda operación de dispersión.
- Idempotency key = `DISB-{disbursementId}` → expira en 24h en Wompi.

## Notas

- Los comentarios en el código backend deben estar en español (regla del proyecto).
- La comisión de Wompi recaudo para pagos manuales (BANK_TRANSFER, CASH_ON_DELIVERY) es $0 porque no pasan por Wompi.
- El `platformCommissionRate` se almacena por comisión individual para soportar tarifas diferenciadas por tenant en el futuro.
- En Fase 1 (MVP), se implementa `CalculateCommissionUseCase` integrado con F006/F007. Los endpoints de consulta y el cron de dispersión se implementan en Fase 2.
- Montos en Wompi Payouts siempre en centavos (ej: $50,000 COP = 5000000).
- Horarios de procesamiento ACH: Bancolombia/Nequi = inmediato. Otros bancos = ciclos ACH (consultar horarios Wompi).
