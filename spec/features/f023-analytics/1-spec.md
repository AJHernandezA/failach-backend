# F023 — Analytics Básico (Backend)

## Propósito

Soporte backend para analytics: almacenar el Measurement ID de GA4 por tenant en la configuración. La implementación de analytics es 100% frontend; el backend solo almacena la configuración.

## Cambios en Domain Layer

### TenantConfig (modificar)

Agregar campo opcional:

- `analyticsId` (String, nullable) — Google Analytics 4 Measurement ID (ej: `G-XXXXXXXXXX`). Si es null, el FE no carga analytics.

## Endpoints Afectados

No se crean endpoints nuevos. Se reutilizan:

| Método | Ruta                                    | Cambio                                    | Auth                   |
| ------ | --------------------------------------- | ----------------------------------------- | ---------------------- |
| `GET`  | `/api/v1/tenants/:tenantId/config`      | Incluye `analyticsId` en la respuesta     | No                     |
| `PUT`  | `/api/v1/tenants/:tenantId/config`      | Acepta `analyticsId` en el body           | SUPER_ADMIN / MERCHANT |

## DynamoDB Access Patterns

Sin cambios. El campo `analyticsId` se almacena como atributo adicional en el registro de configuración del tenant existente.

| Operación         | PK            | SK       | Cambio                           |
| ----------------- | ------------- | -------- | -------------------------------- |
| Get tenant config | `TENANT#<id>` | `CONFIG` | Incluye `analyticsId` si existe  |

## Validación

- `analyticsId` debe coincidir con el formato `G-XXXXXXXXXX` (regex: `^G-[A-Z0-9]{10}$`) si se proporciona.
- Campo nullable — si no se envía o es null, se omite de la respuesta.

## Criterios de Aceptación

- [ ] Campo `analyticsId` se serializa/deserializa correctamente en TenantConfig.
- [ ] GET config retorna `analyticsId` cuando existe.
- [ ] GET config omite `analyticsId` cuando es null.
- [ ] PUT config permite actualizar `analyticsId` (auth requerida).
- [ ] Validación de formato del Measurement ID.

## Notas

- Cambio mínimo en BE — solo agregar un campo a TenantConfig.
- Todo el tracking de eventos se ejecuta en el frontend.
- En Phase 2, se podría agregar tracking server-side (ej: Measurement Protocol de GA4 para eventos de webhook como confirmación de pago).
