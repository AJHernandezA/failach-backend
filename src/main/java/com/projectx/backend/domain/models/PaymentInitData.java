package com.projectx.backend.domain.models;

/**
 * Datos necesarios para inicializar el widget de Wompi en el frontend.
 *
 * @param reference       referencia de pago (PX-tenantId-orderCode)
 * @param amountInCents   monto en centavos
 * @param currency        moneda (COP)
 * @param integritySignature firma de integridad SHA256
 * @param publicKey       llave pública de Wompi del tenant
 * @param redirectUrl     URL de redirección post-pago
 */
public record PaymentInitData(
        String reference,
        long amountInCents,
        String currency,
        String integritySignature,
        String publicKey,
        String redirectUrl
) {}
