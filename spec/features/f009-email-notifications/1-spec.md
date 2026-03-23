# F009 — Email Notifications (Backend)

## Propósito

Implementar el envío de emails transaccionales con AWS SES: confirmación de orden, instrucciones de pago, actualización de estado. Templates HTML personalizados por tenant.

## Domain Layer

### Ports

**EmailService**

- `void sendOrderConfirmation(Tenant tenant, Order order)` — Email de confirmación de orden
- `void sendPaymentInstructions(Tenant tenant, Order order)` — Datos bancarios (usa bankInfo del tenant)
- `void sendStatusUpdate(Tenant tenant, Order order, OrderStatus newStatus)` — Cambio de estado
- `void sendOrderCancellation(Tenant tenant, Order order, String reason)` — Cancelación

### Use Cases

| Interface                             | Input                                                 | Output |
| ------------------------------------- | ----------------------------------------------------- | ------ |
| `SendOrderConfirmationEmailUseCase`   | `String tenantId, Order order`                        | `void` |
| `SendPaymentInstructionsEmailUseCase` | `String tenantId, Order order`                        | `void` |
| `SendOrderStatusUpdateEmailUseCase`   | `String tenantId, Order order, OrderStatus newStatus` | `void` |

## Infrastructure Layer

### SesEmailService

- Implementa `EmailService`.
- Usa AWS SES SDK v2 para enviar emails.
- Carga templates HTML desde resources del proyecto.
- Reemplaza placeholders: `{{tenantName}}`, `{{orderCode}}`, `{{total}}`, etc.
- Aplica colores del tenant al template.
- Email remitente: configurable por env var `SES_FROM_EMAIL`.

### Templates HTML

Ubicación: `src/main/resources/templates/email/`

| Template                    | Uso                                |
| --------------------------- | ---------------------------------- |
| `order-confirmation.html`   | Confirmación de orden con resumen  |
| `payment-instructions.html` | Datos bancarios para transferencia |
| `status-update.html`        | Actualización de estado del pedido |
| `order-cancellation.html`   | Notificación de cancelación        |

### Placeholders Comunes

```html
{{tenantName}} → Nombre del comercio {{tenantLogo}} → URL del logo
{{primaryColor}} → Color primario del tenant {{orderCode}} → Código de la orden
{{customerName}} → Nombre del comprador {{items}} → HTML de los items de la
orden {{total}} → Total formateado ($XX,XXX COP) {{trackingUrl}} → URL de
tracking de la orden {{whatsappUrl}} → URL de WhatsApp del comercio
```

## Criterios de Aceptación

- [x] Al crear orden, se envía email de confirmación al comprador.
- [x] Al elegir transferencia, se envía email con datos bancarios.
- [x] Al cambiar estado de la orden, se envía email de actualización.
- [x] Al cancelar orden, se envía email de cancelación con motivo.
- [x] Emails muestran logo y colores del tenant.
- [x] Emails se ven correctos en Gmail, Outlook y móvil (inline CSS).
- [x] Si SES falla, se loguea el error pero no se bloquea la operación principal.
- [x] Emails se envían de forma asíncrona (virtual threads).
- [x] LogEmailService para desarrollo (no requiere SES).
- [x] SesEmailService para producción (requiere email.enabled=true).

## Notas

- En desarrollo (SES sandbox), solo se puede enviar a emails verificados.
- Los emails se envían de forma asíncrona (virtual thread o CompletableFuture) para no bloquear el response al usuario.
- Si SES falla, loguear WARNING pero NO lanzar excepción al usuario. La orden ya se creó correctamente.
- Templates HTML deben usar inline CSS (no `<style>` tags) para compatibilidad con clientes de email.

## Testing

- `EmailTemplateEngineTest` — Renderizado de 4 templates, placeholders, items HTML, formateo de precios.
- `LogEmailServiceTest` — Todos los métodos se ejecutan sin error.
- `CreateOrderUseCaseImplTest` — Actualizado con mocks de EmailService y TenantRepository.
- `UpdateOrderStatusUseCaseImplTest` — Actualizado con mocks de EmailService y TenantRepository.
