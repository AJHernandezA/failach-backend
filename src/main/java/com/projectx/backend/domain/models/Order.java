package com.projectx.backend.domain.models;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Orden de compra creada a partir del carrito.
 *
 * @param orderId        UUID interno
 * @param orderCode      código legible (ORD-XXXX-XXXX)
 * @param tenantId       tenant al que pertenece
 * @param customer       datos del comprador
 * @param items          productos de la orden
 * @param deliveryMethod método de entrega (SHIPPING / PICKUP)
 * @param deliveryInfo   dirección de envío (null si PICKUP)
 * @param paymentMethod  método de pago
 * @param paymentStatus  estado del pago
 * @param orderStatus    estado de la orden
 * @param subtotal       subtotal sin envío
 * @param shippingCost   costo de envío (puede ser null/0)
 * @param total          total final
 * @param statusHistory  historial de cambios de estado
 * @param notes          notas del comprador
 * @param createdAt      fecha de creación
 * @param updatedAt      última actualización
 */
public record Order(
        String orderId,
        String orderCode,
        String tenantId,
        Customer customer,
        List<OrderItem> items,
        DeliveryMethod deliveryMethod,
        DeliveryInfo deliveryInfo,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        OrderStatus orderStatus,
        BigDecimal subtotal,
        BigDecimal shippingCost,
        BigDecimal total,
        BigDecimal manualPaymentDiscount,
        BigDecimal manualPaymentDiscountRate,
        boolean freeShippingApplied,
        List<StatusHistory> statusHistory,
        String notes,
        Instant createdAt,
        Instant updatedAt) {

    /**
     * Retorna la cantidad total de ítems en la orden.
     */
    public int itemCount() {
        return items.stream().mapToInt(OrderItem::quantity).sum();
    }
}
