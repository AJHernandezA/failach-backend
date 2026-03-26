package com.projectx.backend.domain.models;

/**
 * Datos verificados de una transacción consultada directamente a la API de Wompi.
 * Se usa para validar server-side que el pago es legítimo antes de actualizar la orden.
 *
 * @param transactionId  ID de la transacción en Wompi
 * @param status         estado real de la transacción (APPROVED, DECLINED, VOIDED, ERROR)
 * @param reference      referencia de pago (PX-tenantId-orderCode)
 * @param amountInCents  monto real en centavos
 * @param currency       moneda (COP)
 * @param paymentMethod  método de pago usado (CARD, NEQUI, PSE, etc.)
 */
public record TransactionVerification(
        String transactionId,
        String status,
        String reference,
        long amountInCents,
        String currency,
        String paymentMethod
) {}
