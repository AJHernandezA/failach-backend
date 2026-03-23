package com.projectx.backend.application.usecases;

import com.google.inject.Inject;
import com.projectx.backend.application.dto.UpdateStatusRequest;
import com.projectx.backend.domain.exceptions.BusinessRuleException;
import com.projectx.backend.domain.exceptions.NotFoundException;
import com.projectx.backend.domain.models.*;
import com.projectx.backend.domain.ports.in.UpdateOrderStatusUseCase;
import com.projectx.backend.domain.ports.out.EmailService;
import com.projectx.backend.domain.ports.out.OrderRepository;
import com.projectx.backend.domain.ports.out.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;

/**
 * Actualiza el estado de una orden validando que la transición sea válida.
 * Transiciones permitidas:
 * PENDING → CONFIRMED, CANCELLED
 * CONFIRMED → PREPARING, CANCELLED
 * PREPARING → SHIPPED, CANCELLED
 * SHIPPED → DELIVERED, CANCELLED
 * DELIVERED → (ninguna)
 * CANCELLED → (ninguna)
 */
public class UpdateOrderStatusUseCaseImpl implements UpdateOrderStatusUseCase {

    private static final Logger log = LoggerFactory.getLogger(UpdateOrderStatusUseCaseImpl.class);

    /** Mapa de transiciones válidas por estado actual */
    private static final Map<OrderStatus, Set<OrderStatus>> VALID_TRANSITIONS = Map.of(
            OrderStatus.PENDING, Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
            OrderStatus.CONFIRMED, Set.of(OrderStatus.PREPARING, OrderStatus.CANCELLED),
            OrderStatus.PREPARING, Set.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED),
            OrderStatus.SHIPPED, Set.of(OrderStatus.DELIVERED, OrderStatus.CANCELLED),
            OrderStatus.DELIVERED, Set.of(),
            OrderStatus.CANCELLED, Set.of());

    private final OrderRepository orderRepository;
    private final TenantRepository tenantRepository;
    private final EmailService emailService;

    @Inject
    public UpdateOrderStatusUseCaseImpl(OrderRepository orderRepository,
            TenantRepository tenantRepository,
            EmailService emailService) {
        this.orderRepository = orderRepository;
        this.tenantRepository = tenantRepository;
        this.emailService = emailService;
    }

    @Override
    public Order execute(UpdateStatusRequest request) {
        // 1. Buscar orden
        Order order = orderRepository.findByCode(request.tenantId(), request.orderCode())
                .orElseThrow(() -> new NotFoundException("Orden no encontrada: " + request.orderCode()));

        // 2. Validar transición
        OrderStatus current = order.orderStatus();
        OrderStatus target = request.newStatus();

        Set<OrderStatus> allowed = VALID_TRANSITIONS.getOrDefault(current, Set.of());
        if (!allowed.contains(target)) {
            throw new BusinessRuleException(
                    String.format("No se puede cambiar de %s a %s", current, target));
        }

        // 3. Actualizar orden
        Instant now = Instant.now();
        List<StatusHistory> history = new ArrayList<>(order.statusHistory());
        history.add(new StatusHistory(target, now, request.note()));

        Order updated = new Order(
                order.orderId(), order.orderCode(), order.tenantId(),
                order.customer(), order.items(), order.deliveryMethod(), order.deliveryInfo(),
                order.paymentMethod(), order.paymentStatus(), target,
                order.subtotal(), order.shippingCost(), order.total(),
                order.manualPaymentDiscount(), order.manualPaymentDiscountRate(), order.freeShippingApplied(),
                history, order.notes(), order.createdAt(), now);
        orderRepository.update(updated);

        log.info("Estado de orden {} actualizado: {} → {}", request.orderCode(), current, target);

        // 4. Enviar email de forma asíncrona (F009)
        final Order emailOrder = updated;
        Thread.startVirtualThread(() -> {
            try {
                tenantRepository.findById(request.tenantId()).ifPresent(tenant -> {
                    if (target == OrderStatus.CANCELLED) {
                        emailService.sendOrderCancellation(tenant, emailOrder, request.note());
                    } else {
                        emailService.sendStatusUpdate(tenant, emailOrder, target);
                    }
                });
            } catch (Exception e) {
                log.warn("Error enviando email de estado para orden {}: {}", request.orderCode(), e.getMessage());
            }
        });

        return updated;
    }
}
