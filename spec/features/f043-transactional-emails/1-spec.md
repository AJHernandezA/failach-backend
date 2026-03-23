# F043 — Transactional Email Templates (Backend Spec)

## Servicio principal

### `EmailTemplateService`

Servicio que renderiza templates HTML con datos dinámicos del tenant y del pedido.

**Ubicación**: `infrastructure/email/EmailTemplateService.java`

**Método principal**:
```java
// Renderiza un template HTML con los datos proporcionados
public String render(String templateName, Map<String, Object> data)
```

**Templates** (archivos en `src/main/resources/templates/email/`):

| Template | Disparado por | Destinatario |
|----------|--------------|-------------|
| `order-confirmation.html` | Crear pedido (F005) | Comprador |
| `payment-instructions.html` | Pago por transferencia (F005/F007) | Comprador |
| `order-status-update.html` | Cambiar estado (F037) | Comprador |
| `order-cancelled.html` | Cancelar pedido (F037) | Comprador |
| `new-order-notification.html` | Nuevo pedido recibido | Vendedor |
| `welcome-seller.html` | Registro de vendedor (F033) | Vendedor |
| `store-approved.html` | Aprobar tienda (F041) | Vendedor |

### `EmailSenderService`

Servicio que envía emails via AWS SES.

**Ubicación**: `infrastructure/email/EmailSenderService.java`

**Métodos**:
```java
// Envía un email HTML
public void sendHtml(String toEmail, String subject, String htmlBody)

// Envía email asíncrono (no bloquea el request)
public CompletableFuture<Void> sendHtmlAsync(String toEmail, String subject, String htmlBody)
```

**Configuración**:
- From: `noreply@projectx.com` (configurable via env var `SES_FROM_EMAIL`)
- Reply-To: email del vendedor (configurable por tenant)
- AWS SES SDK v2

## Templates HTML

### Estructura común de todos los templates

```html
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <style>
    /* Estilos inline y en <style> para compatibilidad con clientes de email */
    /* Colores dinámicos del tenant: {{primaryColor}}, {{secondaryColor}} */
  </style>
</head>
<body>
  <!-- Header con logo del tenant -->
  <table>
    <tr>
      <td><img src="{{logoUrl}}" alt="{{businessName}}" /></td>
    </tr>
  </table>

  <!-- Contenido del email -->
  {{content}}

  <!-- Footer con datos de contacto -->
  <table>
    <tr>
      <td>
        {{businessName}} | {{phone}} | {{email}}
        <br>Powered by Project-X
      </td>
    </tr>
  </table>
</body>
</html>
```

### Variables comunes en todos los templates

| Variable | Descripción |
|----------|-------------|
| `{{businessName}}` | Nombre del negocio del tenant |
| `{{logoUrl}}` | URL del logo del tenant |
| `{{primaryColor}}` | Color primario del tenant (hex) |
| `{{secondaryColor}}` | Color secundario del tenant (hex) |
| `{{phone}}` | Teléfono del negocio |
| `{{email}}` | Email del negocio |
| `{{storeUrl}}` | URL de la tienda |

### Template: `order-confirmation.html`

Variables adicionales:

| Variable | Descripción |
|----------|-------------|
| `{{customerName}}` | Nombre del comprador |
| `{{orderCode}}` | Código de la orden |
| `{{items}}` | Lista de items (nombre, cantidad, precio, subtotal) |
| `{{subtotal}}` | Subtotal de la orden |
| `{{shippingCost}}` | Costo de envío |
| `{{discount}}` | Descuento aplicado |
| `{{totalAmount}}` | Monto total |
| `{{paymentMethod}}` | Método de pago |
| `{{trackingUrl}}` | URL de tracking de la orden |
| `{{deliveryAddress}}` | Dirección de entrega |

### Template: `payment-instructions.html`

Variables adicionales:

| Variable | Descripción |
|----------|-------------|
| `{{bankName}}` | Nombre del banco |
| `{{accountType}}` | Tipo de cuenta |
| `{{accountNumber}}` | Número de cuenta |
| `{{accountHolder}}` | Titular de la cuenta |
| `{{amountToPay}}` | Monto a transferir (formateado COP) |
| `{{orderCode}}` | Referencia a incluir en la transferencia |

### Template: `order-status-update.html`

Variables adicionales:

| Variable | Descripción |
|----------|-------------|
| `{{newStatus}}` | Nuevo estado (texto legible) |
| `{{statusNote}}` | Nota del vendedor |
| `{{trackingUrl}}` | URL de tracking |
| `{{statusIcon}}` | Emoji/ícono del estado |

### Template: `new-order-notification.html`

Variables adicionales (para el vendedor):

| Variable | Descripción |
|----------|-------------|
| `{{customerName}}` | Nombre del comprador |
| `{{customerPhone}}` | Teléfono del comprador |
| `{{orderCode}}` | Código de la orden |
| `{{items}}` | Lista de items |
| `{{totalAmount}}` | Monto total |
| `{{paymentMethod}}` | Método de pago |
| `{{dashboardUrl}}` | Link al detalle del pedido en el dashboard |

## Integración con otros features

### F005 (Checkout) — Al crear pedido:
```java
// En CreateOrderUseCase
emailTemplateService.sendOrderConfirmation(order, tenant);
if (order.getPaymentMethod() == BANK_TRANSFER) {
    emailTemplateService.sendPaymentInstructions(order, tenant);
}
emailTemplateService.sendNewOrderNotification(order, tenant, sellerEmail);
```

### F037 (Order Management) — Al cambiar estado:
```java
// En UpdateOrderStatusUseCase
emailTemplateService.sendOrderStatusUpdate(order, newStatus, note, tenant);
```

### F037 — Al cancelar pedido:
```java
emailTemplateService.sendOrderCancelled(order, reason, tenant);
```

### F033 (Auth) — Al registrar vendedor:
```java
emailTemplateService.sendWelcomeSeller(user, tenant);
```

### F041 (Store Settings) — Al aprobar tienda:
```java
emailTemplateService.sendStoreApproved(tenant, sellerEmail);
```

## Arquitectura (Hexagonal)

```
infrastructure/
├── email/
│   ├── EmailTemplateService.java    // Renderiza templates
│   ├── EmailSenderService.java      // Envía via SES
│   └── SesEmailSender.java          // Implementación SES
├── persistence/
│   └── DynamoEmailLogRepository.java // Log de emails enviados
application/
├── usecase/
│   └── (Se integra en los use cases de otros features)
domain/
├── model/
│   └── EmailLog.java                // Registro de email enviado
├── port/
│   ├── EmailSender.java             // Puerto de envío
│   └── EmailLogRepository.java      // Puerto de logging
```

## DynamoDB (Log de emails)

| PK | SK | Datos |
|----|-----|-------|
| `TENANT#<tenantId>` | `EMAIL_LOG#<timestamp>#<id>` | templateName, recipientEmail, subject, status (SENT/FAILED), relatedEntity, sentAt |

## Variables de entorno

| Variable | Descripción |
|----------|-------------|
| `SES_FROM_EMAIL` | Email remitente (noreply@projectx.com) |
| `EMAIL_ENABLED` | `true`/`false` — permite deshabilitar envío en desarrollo |
| `AWS_REGION` | Región de SES |

## Comentarios en código

Sí, todos los archivos Java deben tener comentarios explicatorios en español.

## Notas

- Los templates HTML usan tablas para layout (estándar en email HTML para compatibilidad).
- Los estilos son inline o en `<style>` — no se pueden usar CSS externo en emails.
- Los emails se envían de forma asíncrona (`CompletableFuture`) para no bloquear el request principal.
- Si `EMAIL_ENABLED=false`, se logea el email pero no se envía (útil en desarrollo).
- El log de emails permite auditoría y debugging de envíos fallidos.
- Usar un motor de templates simple: `String.replace()` o una librería ligera como Mustache.java.
- Los emails deben testearse con https://www.mail-tester.com/ para verificar que no caen en spam.
- El footer de todos los emails dice "Powered by Project-X" con link a la landing page.
