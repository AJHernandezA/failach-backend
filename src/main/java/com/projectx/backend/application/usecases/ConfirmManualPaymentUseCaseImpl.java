package com.projectx.backend.application.usecases;

import com.google.inject.Inject;
import com.projectx.backend.domain.exceptions.BadRequestException;
import com.projectx.backend.domain.exceptions.BusinessRuleException;
import com.projectx.backend.domain.exceptions.NotFoundException;
import com.projectx.backend.domain.models.*;
import com.projectx.backend.domain.ports.in.ConfirmManualPaymentUseCase;
import com.projectx.backend.domain.ports.out.OrderRepository;
import com.projectx.backend.domain.ports.out.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Confirma el pago manual de una orden.
 * Para transferencia bancaria: descuenta stock al confirmar.
 * Para efectivo contraentrega: stock ya fue descontado al crear la orden.
 */
public class ConfirmManualPaymentUseCaseImpl implements ConfirmManualPaymentUseCase {

    private static final Logger log = LoggerFactory.getLogger(ConfirmManualPaymentUseCaseImpl.class);

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @Inject
    public ConfirmManualPaymentUseCaseImpl(OrderRepository orderRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    @Override
    public Order execute(String tenantId, String orderCode, String note) {
        // 1. Buscar orden
        Order order = orderRepository.findByCode(tenantId, orderCode)
                .orElseThrow(() -> new NotFoundException("Orden no encontrada: " + orderCode));

        // 2. Validar método de pago manual
        if (order.paymentMethod() != PaymentMethod.BANK_TRANSFER
                && order.paymentMethod() != PaymentMethod.CASH_ON_DELIVERY) {
            throw new BadRequestException("Solo se pueden confirmar pagos manuales (transferencia o efectivo)");
        }

        // 3. Validar que esté pendiente
        if (order.paymentStatus() == PaymentStatus.PAID) {
            throw new BusinessRuleException("La orden ya está pagada");
        }

        if (order.paymentStatus() != PaymentStatus.PENDING) {
            throw new BadRequestException(
                    "No se puede confirmar una orden con estado de pago: " + order.paymentStatus());
        }

        // 4. Actualizar estado
        Instant now = Instant.now();
        List<StatusHistory> history = new ArrayList<>(order.statusHistory());
        history.add(new StatusHistory(OrderStatus.CONFIRMED, now,
                note != null ? note : "Pago manual confirmado"));

        Order updated = new Order(
                order.orderId(), order.orderCode(), order.tenantId(),
                order.customer(), order.items(), order.deliveryMethod(), order.deliveryInfo(),
                order.paymentMethod(), PaymentStatus.PAID, OrderStatus.CONFIRMED,
                order.subtotal(), order.shippingCost(), order.total(),
                order.manualPaymentDiscount(), order.manualPaymentDiscountRate(), order.freeShippingApplied(),
                history, order.notes(), order.createdAt(), now);
        orderRepository.update(updated);

        // 5. Si es transferencia bancaria, descontar stock ahora
        if (order.paymentMethod() == PaymentMethod.BANK_TRANSFER) {
            for (OrderItem item : order.items()) {
                productRepository.findById(tenantId, item.productId()).ifPresent(product -> {
                    int newStock = product.stock() - item.quantity();
                    Product updatedProduct = new Product(
                            product.productId(), product.tenantId(), product.name(),
                            product.description(), product.price(), product.compareAtPrice(),
                            product.images(), product.categoryId(), product.categoryName(),
                            Math.max(0, newStock), product.variants(),
                            newStock <= 0 ? ProductStatus.OUT_OF_STOCK : product.status(),
                            product.sortOrder(), product.createdAt(), Instant.now());
                    productRepository.save(updatedProduct);
                });
            }
            log.info("Stock descontado para orden de transferencia: {}", orderCode);
        }

        log.info("Pago manual confirmado para orden: {}", orderCode);
        return updated;
    }
}
