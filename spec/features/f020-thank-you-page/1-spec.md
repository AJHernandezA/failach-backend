# F020 — Thank You Page (Backend)

## Propósito

Soporte backend para la página de agradecimiento post-compra. Principalmente reutiliza endpoints existentes (F008 order tracking). Agrega campo opcional `thankYouMessage` a la configuración del tenant.

## Endpoints

No se crean endpoints nuevos. Se reutilizan:

| Método | Ruta                                       | Descripción                    | Feature origen |
| ------ | ------------------------------------------ | ------------------------------ | -------------- |
| `GET`  | `/api/v1/tenants/:tenantId/orders/:code`   | Obtener datos del pedido       | F008           |
| `GET`  | `/api/v1/tenants/:tenantId/config`         | Obtener config del tenant      | F002           |

## Cambios en Domain Layer

### TenantConfig (modificar)

Agregar campo opcional:

- `thankYouMessage` (String, nullable) — Mensaje de agradecimiento personalizado en markdown. Si es null, el FE usa template por defecto.

## DynamoDB Access Patterns

Sin cambios. El campo `thankYouMessage` se almacena como atributo adicional en el registro de configuración del tenant existente.

| Operación        | PK            | SK       | Cambio                            |
| ---------------- | ------------- | -------- | --------------------------------- |
| Get tenant config | `TENANT#<id>` | `CONFIG` | Incluye `thankYouMessage` si existe |

## Criterios de Aceptación

- [ ] Campo `thankYouMessage` se serializa/deserializa correctamente en TenantConfig.
- [ ] GET config retorna `thankYouMessage` cuando existe.
- [ ] GET config retorna `null` para `thankYouMessage` cuando no está configurado.
- [ ] PUT config permite actualizar `thankYouMessage` (SUPER_ADMIN / MERCHANT).

## Notas

- Cambio mínimo en BE — la mayor parte de la lógica de Thank You Page es frontend.
- El campo es opcional para no romper tenants existentes.
