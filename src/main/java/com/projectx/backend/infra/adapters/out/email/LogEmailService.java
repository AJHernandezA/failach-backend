package com.projectx.backend.infra.adapters.out.email;

import com.projectx.backend.domain.models.Order;
import com.projectx.backend.domain.models.OrderStatus;
import com.projectx.backend.domain.models.Tenant;
import com.projectx.backend.domain.ports.out.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Implementación de EmailService para desarrollo.
 * Solo loguea los emails en vez de enviarlos realmente.
 * Útil cuando no hay AWS SES configurado.
 */
public class LogEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(LogEmailService.class);

    private static final Map<OrderStatus, String> STATUS_LABELS = Map.of(
            OrderStatus.PENDING, "Pedido recibido",
            OrderStatus.CONFIRMED, "Pago confirmado",
            OrderStatus.PREPARING, "En preparación",
            OrderStatus.SHIPPED, "En camino",
            OrderStatus.DELIVERED, "Entregado",
            OrderStatus.CANCELLED, "Cancelado"
    );

    @Override
    public void sendOrderConfirmation(Tenant tenant, Order order) {
        log.info("[EMAIL-LOG] Confirmación de orden → {} | Orden: {} | Total: {} | Tenant: {}",
                order.customer().email(), order.orderCode(),
                EmailTemplateEngine.formatPrice(order.total()), tenant.name());
    }

    @Override
    public void sendPaymentInstructions(Tenant tenant, Order order) {
        log.info("[EMAIL-LOG] Instrucciones de pago → {} | Orden: {} | Total: {} | Tenant: {}",
                order.customer().email(), order.orderCode(),
                EmailTemplateEngine.formatPrice(order.total()), tenant.name());
    }

    @Override
    public void sendStatusUpdate(Tenant tenant, Order order, OrderStatus newStatus) {
        log.info("[EMAIL-LOG] Actualización de estado → {} | Orden: {} | Estado: {} | Tenant: {}",
                order.customer().email(), order.orderCode(),
                STATUS_LABELS.getOrDefault(newStatus, newStatus.name()), tenant.name());
    }

    @Override
    public void sendOrderCancellation(Tenant tenant, Order order, String reason) {
        log.info("[EMAIL-LOG] Cancelación de orden → {} | Orden: {} | Motivo: {} | Tenant: {}",
                order.customer().email(), order.orderCode(), reason, tenant.name());
    }
}
