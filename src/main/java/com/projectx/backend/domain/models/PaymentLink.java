package com.projectx.backend.domain.models;

/**
 * Modelo de dominio para un link de pago de Wompi.
 *
 * @param id           ID del link generado por Wompi
 * @param name         Nombre descriptivo del link
 * @param description  Descripción del pago
 * @param singleUse    Si solo acepta un pago aprobado
 * @param amountInCents Monto fijo en centavos (null = monto abierto)
 * @param currency     Moneda (COP)
 * @param imageUrl     URL de la imagen/logo de la tienda
 * @param checkoutUrl  URL completa del checkout (https://checkout.wompi.co/l/{id})
 * @param active       Si el link está activo
 */
public record PaymentLink(
        String id,
        String name,
        String description,
        boolean singleUse,
        Long amountInCents,
        String currency,
        String imageUrl,
        String checkoutUrl,
        boolean active
) {}
