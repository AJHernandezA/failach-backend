package com.projectx.backend.domain.models;

/**
 * Información de entrega para órdenes con envío a domicilio.
 *
 * @param address        dirección de entrega
 * @param city           ciudad
 * @param neighborhood   barrio (opcional)
 * @param additionalInfo información adicional (apto, torre, etc.)
 */
public record DeliveryInfo(
        String address,
        String city,
        String neighborhood,
        String additionalInfo
) {}
