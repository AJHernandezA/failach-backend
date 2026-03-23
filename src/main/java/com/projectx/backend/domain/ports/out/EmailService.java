package com.projectx.backend.domain.ports.out;

import com.projectx.backend.domain.models.Order;
import com.projectx.backend.domain.models.OrderStatus;
import com.projectx.backend.domain.models.Tenant;

/**
 * Puerto de salida para el envío de emails transaccionales.
 * Las implementaciones pueden usar AWS SES, SMTP, o simplemente loguear (desarrollo).
 */
public interface EmailService {

    /**
     * Envía email de confirmación de orden al comprador.
     */
    void sendOrderConfirmation(Tenant tenant, Order order);

    /**
     * Envía email con instrucciones de pago por transferencia bancaria.
     */
    void sendPaymentInstructions(Tenant tenant, Order order);

    /**
     * Envía email de actualización de estado de la orden.
     */
    void sendStatusUpdate(Tenant tenant, Order order, OrderStatus newStatus);

    /**
     * Envía email de cancelación de orden.
     */
    void sendOrderCancellation(Tenant tenant, Order order, String reason);
}
