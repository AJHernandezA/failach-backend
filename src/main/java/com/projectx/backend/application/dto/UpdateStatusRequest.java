package com.projectx.backend.application.dto;

import com.projectx.backend.domain.models.OrderStatus;

/**
 * Request para actualizar el estado de una orden.
 *
 * @param tenantId   tenant al que pertenece la orden
 * @param orderCode  código público de la orden
 * @param newStatus  nuevo estado deseado
 * @param note       nota opcional del comercio
 */
public record UpdateStatusRequest(
        String tenantId,
        String orderCode,
        OrderStatus newStatus,
        String note
) {}
