package com.projectx.backend.domain.models;

/**
 * Filtros para listar órdenes de un tenant.
 *
 * @param orderStatus  filtrar por estado de orden (nullable)
 * @param paymentStatus filtrar por estado de pago (nullable)
 * @param page         número de página (0-indexed)
 * @param size         tamaño de página
 */
public record OrderFilter(
        OrderStatus orderStatus,
        PaymentStatus paymentStatus,
        int page,
        int size
) {
    public OrderFilter {
        if (page < 0) page = 0;
        if (size <= 0) size = 20;
    }
}
