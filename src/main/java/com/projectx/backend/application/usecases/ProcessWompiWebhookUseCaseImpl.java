package com.projectx.backend.application.usecases;

import com.google.inject.Inject;
import com.projectx.backend.domain.exceptions.BadRequestException;
import com.projectx.backend.domain.models.*;
import com.projectx.backend.domain.ports.in.ProcessWompiWebhookUseCase;
import com.projectx.backend.domain.ports.out.OrderRepository;
import com.projectx.backend.domain.ports.out.PaymentService;
import com.projectx.backend.domain.ports.out.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Procesa eventos de webhook de Wompi (transaction.updated).
 * Verifica firma, actualiza estado de la orden y descuenta stock si
 * corresponde.
 * Implementa idempotencia: si la orden ya está PAID, ignora el evento.
 */
public class ProcessWompiWebhookUseCaseImpl implements ProcessWompiWebhookUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessWompiWebhookUseCaseImpl.class);

    private final OrderRepository orderRepository;
    private final PaymentService paymentService;
    private final ProductRepository productRepository;

    @Inject
    public ProcessWompiWebhookUseCaseImpl(OrderRepository orderRepository,
            PaymentService paymentService,
            ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.paymentService = paymentService;
        this.productRepository = productRepository;
    }

    @Override
    public void execute(WompiWebhookEvent event) {
        // 1. Verificar firma
        boolean valid = paymentService.verifyWebhookSignature(
                event.transactionId(), event.status(), event.amountInCents(), event.signature());
        if (!valid) {
            throw new BadRequestException("Firma de webhook inválida");
        }

        // 2. Extraer orderCode de la referencia (PX-tenantId-orderCode)
        String reference = event.reference();
        String[] parts = reference.split("-", 3);
        if (parts.length < 3 || !parts[0].equals("PX")) {
            log.warn("Referencia inválida en webhook: {}", reference);
            throw new BadRequestException("Referencia de pago inválida");
        }
        String tenantId = parts[1];
        String orderCode = parts[2];

        // 3. Buscar orden
        Order order = orderRepository.findByCode(tenantId, orderCode).orElse(null);
        if (order == null) {
            log.warn("Orden no encontrada para webhook: {}", orderCode);
            return;
        }

        // 4. Idempotencia: si ya está pagada, ignorar
        if (order.paymentStatus() == PaymentStatus.PAID) {
            log.info("Webhook duplicado ignorado para orden ya pagada: {}", orderCode);
            return;
        }

        // 5. Procesar según estado
        Instant now = Instant.now();
        List<StatusHistory> history = new ArrayList<>(order.statusHistory());

        if ("APPROVED".equals(event.status())) {
            // Pago exitoso
            history.add(new StatusHistory(OrderStatus.CONFIRMED, now, "Pago aprobado por Wompi"));

            Order updated = new Order(
                    order.orderId(), order.orderCode(), order.tenantId(),
                    order.customer(), order.items(), order.deliveryMethod(), order.deliveryInfo(),
                    order.paymentMethod(), PaymentStatus.PAID, OrderStatus.CONFIRMED,
                    order.subtotal(), order.shippingCost(), order.total(),
                    order.manualPaymentDiscount(), order.manualPaymentDiscountRate(), order.freeShippingApplied(),
                    history, order.notes(), order.createdAt(), now);
            orderRepository.update(updated);

            // Descontar stock
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

            log.info("Pago aprobado para orden {}", orderCode);

        } else {
            // Pago fallido (DECLINED, VOIDED, ERROR)
            history.add(new StatusHistory(order.orderStatus(), now,
                    "Pago rechazado por Wompi: " + event.status()));

            Order updated = new Order(
                    order.orderId(), order.orderCode(), order.tenantId(),
                    order.customer(), order.items(), order.deliveryMethod(), order.deliveryInfo(),
                    order.paymentMethod(), PaymentStatus.FAILED, order.orderStatus(),
                    order.subtotal(), order.shippingCost(), order.total(),
                    order.manualPaymentDiscount(), order.manualPaymentDiscountRate(), order.freeShippingApplied(),
                    history, order.notes(), order.createdAt(), now);
            orderRepository.update(updated);

            log.info("Pago rechazado para orden {}: {}", orderCode, event.status());
        }
    }
}
