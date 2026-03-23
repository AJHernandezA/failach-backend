package com.projectx.backend.domain.models;

/**
 * Evento recibido del webhook de Wompi (transaction.updated).
 *
 * @param event        tipo de evento (transaction.updated)
 * @param signature    firma del evento para verificación
 * @param timestamp    timestamp del evento
 * @param transactionId ID de la transacción en Wompi
 * @param status       estado de la transacción (APPROVED, DECLINED, VOIDED, ERROR)
 * @param reference    referencia de pago (PX-tenantId-orderCode)
 * @param amountInCents monto en centavos
 */
public record WompiWebhookEvent(
        String event,
        String signature,
        long timestamp,
        String transactionId,
        String status,
        String reference,
        long amountInCents
) {}
