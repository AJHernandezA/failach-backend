# F010 — WhatsApp Message Generation (Backend)

## Propósito

Generar mensajes pre-armados de WhatsApp para diferentes contextos: consulta general, compartir producto, post-compra, confirmación de transferencia.

## Domain Layer

### Use Cases

| Interface | Input | Output |
|-----------|-------|--------|
| `GenerateWhatsAppMessageUseCase` | `WhatsAppMessageRequest` | `WhatsAppMessageResponse` (url + message) |

### Message Types

| Tipo | Contexto | Contenido |
|------|----------|-----------|
| `GENERAL_INQUIRY` | Botón flotante | "Hola! Estoy visitando su tienda online {tenantName}. Tengo una consulta." |
| `SHARE_PRODUCT` | Compartir producto | "Mira este producto: {productName} - ${price} COP. {productUrl}" |
| `ORDER_SUMMARY` | Post-compra | "Hola! Acabo de hacer un pedido: {orderCode}. Total: ${total}. Método: {paymentMethod}." |
| `TRANSFER_CONFIRM` | Ya transferí | "Hola! Acabo de transferir ${amount} para mi pedido {orderCode}. Mi nombre es {customerName}." |

## Application Layer

### GenerateWhatsAppMessageUseCaseImpl
1. Obtener número de WhatsApp del tenant (desde TenantRepository).
2. Construir mensaje según el tipo.
3. URL encode el mensaje.
4. Retornar: `https://wa.me/{number}?text={encodedMessage}`.

## Criterios de Aceptación

- [ ] Genera URL válida de wa.me con mensaje encoded.
- [ ] Mensaje de consulta general incluye nombre del tenant.
- [ ] Mensaje de compartir producto incluye nombre, precio y URL.
- [ ] Mensaje post-compra incluye orderCode y total.
- [ ] Mensaje de transferencia incluye orderCode, monto y nombre.
- [ ] Número sin +, sin espacios, solo dígitos con código de país.

## Notas

- Este use case puede ser llamado directamente desde el frontend sin endpoint dedicado (el FE tiene toda la info para construir los mensajes).
- Se implementa como endpoint del BE solo si el FE necesita que el BE genere los mensajes (por ejemplo, si los templates cambian frecuentemente).
- Para MVP: el FE puede generar los mensajes localmente con `lib/whatsapp.ts`. El BE solo provee el número de WhatsApp via tenant config.
