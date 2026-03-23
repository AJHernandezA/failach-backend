package com.projectx.backend.domain.models;

/**
 * Respuesta del estado de pago de una orden.
 *
 * @param orderCode     código de la orden
 * @param paymentStatus estado del pago
 * @param orderStatus   estado de la orden
 * @param paymentMethod método de pago usado
 */
public record PaymentStatusResponse(
        String orderCode,
        PaymentStatus paymentStatus,
        OrderStatus orderStatus,
        PaymentMethod paymentMethod
) {}
