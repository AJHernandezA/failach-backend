package com.projectx.backend.application.usecases;

import com.google.inject.Inject;
import com.projectx.backend.domain.exceptions.NotFoundException;
import com.projectx.backend.domain.models.Order;
import com.projectx.backend.domain.models.PaymentStatusResponse;
import com.projectx.backend.domain.ports.in.GetPaymentStatusUseCase;
import com.projectx.backend.domain.ports.out.OrderRepository;

/**
 * Retorna el estado del pago de una orden.
 */
public class GetPaymentStatusUseCaseImpl implements GetPaymentStatusUseCase {

    private final OrderRepository orderRepository;

    @Inject
    public GetPaymentStatusUseCaseImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public PaymentStatusResponse execute(String tenantId, String orderCode) {
        Order order = orderRepository.findByCode(tenantId, orderCode)
                .orElseThrow(() -> new NotFoundException("Orden no encontrada: " + orderCode));

        return new PaymentStatusResponse(
                order.orderCode(),
                order.paymentStatus(),
                order.orderStatus(),
                order.paymentMethod()
        );
    }
}
