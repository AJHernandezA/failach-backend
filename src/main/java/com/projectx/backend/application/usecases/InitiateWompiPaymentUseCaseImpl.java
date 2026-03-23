package com.projectx.backend.application.usecases;

import com.google.inject.Inject;
import com.projectx.backend.domain.exceptions.BadRequestException;
import com.projectx.backend.domain.exceptions.NotFoundException;
import com.projectx.backend.domain.models.Order;
import com.projectx.backend.domain.models.PaymentInitData;
import com.projectx.backend.domain.models.PaymentMethod;
import com.projectx.backend.domain.models.PaymentStatus;
import com.projectx.backend.domain.ports.in.InitiateWompiPaymentUseCase;
import com.projectx.backend.domain.ports.out.OrderRepository;
import com.projectx.backend.domain.ports.out.PaymentService;

/**
 * Genera los datos necesarios para inicializar el widget de Wompi.
 * Valida que la orden exista, use método WOMPI y esté pendiente de pago.
 */
public class InitiateWompiPaymentUseCaseImpl implements InitiateWompiPaymentUseCase {

    private final OrderRepository orderRepository;
    private final PaymentService paymentService;

    @Inject
    public InitiateWompiPaymentUseCaseImpl(OrderRepository orderRepository, PaymentService paymentService) {
        this.orderRepository = orderRepository;
        this.paymentService = paymentService;
    }

    @Override
    public PaymentInitData execute(String tenantId, String orderCode, String redirectUrl) {
        // 1. Buscar orden
        Order order = orderRepository.findByCode(tenantId, orderCode)
                .orElseThrow(() -> new NotFoundException("Orden no encontrada: " + orderCode));

        // 2. Validar método de pago
        if (order.paymentMethod() != PaymentMethod.WOMPI) {
            throw new BadRequestException("Esta orden no usa pago Wompi");
        }

        // 3. Validar que esté pendiente
        if (order.paymentStatus() != PaymentStatus.PENDING) {
            throw new BadRequestException("Esta orden ya fue procesada");
        }

        // 4. Generar datos de pago
        return paymentService.initiate(tenantId, order, redirectUrl);
    }
}
