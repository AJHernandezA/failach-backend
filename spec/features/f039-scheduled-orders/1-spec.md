# F039 — Scheduled/Future Orders (Backend Spec)

## Endpoints

### `GET /api/v1/admin/orders/calendar`

Pedidos programados agrupados por fecha para el calendario.

**Auth**: Requerida (MERCHANT, SUPER_ADMIN).

**Query params**:
| Param | Tipo | Descripción |
|-------|------|-------------|
| `month` | String | Mes a consultar (YYYY-MM) |
| `tenantId` | String | Solo SUPER_ADMIN |

**Response** (`200 OK`):
```json
{
  "2026-03-15": [
    {
      "orderCode": "ORD-2026-0045",
      "customerName": "Juan Pérez",
      "timeSlot": "MORNING",
      "status": "CONFIRMED",
      "amount": 63900
    }
  ],
  "2026-03-16": [
    {
      "orderCode": "ORD-2026-0046",
      "customerName": "María López",
      "timeSlot": "AFTERNOON",
      "status": "PREPARING",
      "amount": 45000
    }
  ]
}
```

**Implementación**:
- Query pedidos del tenant donde `isScheduled=true` y `scheduledDate` está en el mes solicitado.
- Agrupar por `scheduledDate`.
- Incluir pedidos no programados que se crearon en ese mes también (para vista completa).

## Extensiones a modelos existentes

### Order (extensión)

Nuevos campos agregados al modelo Order:

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `isScheduled` | Boolean | Si es pedido programado (default: false) |
| `scheduledDate` | String | Fecha programada YYYY-MM-DD (null si no programado) |
| `scheduledTimeSlot` | String | Franja: MORNING, AFTERNOON, EVENING (null si no programado) |

### TenantConfig (extensión)

Nuevos campos en la configuración del tenant:

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `availableDays` | List<String> | Días de operación: MONDAY, TUESDAY, ... SUNDAY |
| `orderCutoffTime` | String | Hora de corte HH:mm (ej: "15:00") |
| `timeSlots` | List<TimeSlot> | Franjas horarias disponibles |
| `maxScheduleDays` | Integer | Máximo días futuros para programar (default: 14) |

### TimeSlot

```json
{
  "key": "MORNING",
  "label": "Mañana",
  "from": "08:00",
  "to": "12:00"
}
```

## Extensión del endpoint de checkout (F005)

El endpoint `POST /api/v1/tenants/:tenantId/orders` ya existente se extiende para aceptar:

```json
{
  "...campos existentes...",
  "isScheduled": true,
  "scheduledDate": "2026-03-20",
  "scheduledTimeSlot": "MORNING"
}
```

**Validaciones nuevas en el backend**:
1. Si `isScheduled=true`, `scheduledDate` y `scheduledTimeSlot` son requeridos.
2. `scheduledDate` debe ser una fecha futura.
3. `scheduledDate` no puede exceder `maxScheduleDays` días desde hoy.
4. El día de la semana de `scheduledDate` debe estar en `availableDays` del tenant.
5. Si la hora actual es posterior a `orderCutoffTime`, no se puede programar para hoy.
6. `scheduledTimeSlot` debe ser una franja válida configurada en el tenant.

## Extensión del endpoint de pedido manual (F037)

El endpoint `POST /api/v1/admin/orders/manual` ya acepta `isScheduled`, `scheduledDate`, `scheduledTimeSlot` según la spec de F037.

## DynamoDB

Nuevo GSI para queries de calendario:

| GSI Name | PK | SK | Proyección |
|----------|----|----|-----------|
| `ScheduleIndex` | `TENANT#<tenantId>` | `SCHEDULE#<scheduledDate>#<orderCode>` | ALL |

O alternativamente, usar el sort key existente con un prefijo para filtrar pedidos programados.

## Arquitectura (Hexagonal)

```
infrastructure/
├── web/
│   └── AdminCalendarController.java
├── persistence/
│   └── DynamoCalendarRepository.java
application/
├── usecase/
│   ├── GetCalendarOrdersUseCase.java
│   └── ValidateScheduleUseCase.java    // Valida fecha y franja
domain/
├── model/
│   ├── TimeSlot.java
│   └── ScheduleValidation.java
├── port/
│   └── CalendarRepository.java
```

## Comentarios en código

Sí, todos los archivos Java deben tener comentarios explicatorios en español.

## Notas

- Las franjas por defecto si el tenant no tiene configuradas: MORNING (8-12), AFTERNOON (12-17), EVENING (17-21).
- Los días disponibles por defecto: MONDAY a SATURDAY (domingo cerrado).
- La hora de corte por defecto: 15:00.
- `maxScheduleDays` por defecto: 14.
- La zona horaria para validaciones es America/Bogota (UTC-5).
- A futuro: capacidad máxima por franja horaria para evitar sobre-venta.
