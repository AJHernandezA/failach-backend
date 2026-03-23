package com.projectx.backend.application.usecases;

import com.google.inject.Inject;
import com.projectx.backend.domain.exceptions.BusinessRuleException;
import com.projectx.backend.domain.exceptions.NotFoundException;
import com.projectx.backend.domain.models.*;
import com.projectx.backend.domain.ports.in.CancelOrderUseCase;
import com.projectx.backend.domain.ports.out.OrderRepository;
import com.projectx.backend.domain.ports.out.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Cancela una orden y restaura stock si fue descontado previamente.
 * No se puede cancelar una orden en estado DELIVERED.
 */
public class CancelOrderUseCaseImpl implements CancelOrderUseCase {

    private static final Logger log = LoggerFactory.getLogger(CancelOrderUseCaseImpl.class);

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @Inject
    public CancelOrderUseCaseImpl(OrderRepository orderRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    @Override
    public Order execute(String tenantId, String orderCode, String reason) {
        // 1. Buscar orden
        Order order = orderRepository.findByCode(tenantId, orderCode)
                .orElseThrow(() -> new NotFoundException("Orden no encontrada: " + orderCode));

        // 2. Validar que no esté entregada
        if (order.orderStatus() == OrderStatus.DELIVERED) {
            throw new BusinessRuleException("No se puede cancelar una orden ya entregada");
        }

        // 3. Validar que no esté ya cancelada
        if (order.orderStatus() == OrderStatus.CANCELLED) {
            throw new BusinessRuleException("La orden ya está cancelada");
        }

        // 4. Restaurar stock si fue descontado
        boolean stockWasDeducted = order.paymentStatus() == PaymentStatus.PAID
                || order.paymentMethod() == PaymentMethod.CASH_ON_DELIVERY;

        if (stockWasDeducted) {
            for (OrderItem item : order.items()) {
                productRepository.findById(tenantId, item.productId()).ifPresent(product -> {
                    int restoredStock = product.stock() + item.quantity();
                    Product updatedProduct = new Product(
                            product.productId(), product.tenantId(), product.name(),
                            product.description(), product.price(), product.compareAtPrice(),
                            product.images(), product.categoryId(), product.categoryName(),
                            restoredStock, product.variants(),
                            restoredStock > 0 ? ProductStatus.ACTIVE : product.status(),
                            product.sortOrder(), product.createdAt(), Instant.now());
                    productRepository.save(updatedProduct);
                });
            }
            log.info("Stock restaurado para orden cancelada: {}", orderCode);
        }

        // 5. Actualizar orden
        Instant now = Instant.now();
        List<StatusHistory> history = new ArrayList<>(order.statusHistory());
        history.add(new StatusHistory(OrderStatus.CANCELLED, now,
                reason != null ? reason : "Orden cancelada"));

        Order cancelled = new Order(
                order.orderId(), order.orderCode(), order.tenantId(),
                order.customer(), order.items(), order.deliveryMethod(), order.deliveryInfo(),
                order.paymentMethod(), order.paymentStatus(), OrderStatus.CANCELLED,
                order.subtotal(), order.shippingCost(), order.total(),
                order.manualPaymentDiscount(), order.manualPaymentDiscountRate(), order.freeShippingApplied(),
                history, order.notes(), order.createdAt(), now);
        orderRepository.update(cancelled);

        log.info("Orden cancelada: {}", orderCode);
        return cancelled;
    }
}
