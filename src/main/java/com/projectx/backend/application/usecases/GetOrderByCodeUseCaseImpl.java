package com.projectx.backend.application.usecases;

import com.google.inject.Inject;
import com.projectx.backend.domain.exceptions.NotFoundException;
import com.projectx.backend.domain.models.Order;
import com.projectx.backend.domain.ports.in.GetOrderByCodeUseCase;
import com.projectx.backend.domain.ports.out.OrderRepository;

/**
 * Implementación del caso de uso para obtener una orden por su código legible.
 */
public class GetOrderByCodeUseCaseImpl implements GetOrderByCodeUseCase {

    private final OrderRepository orderRepository;

    @Inject
    public GetOrderByCodeUseCaseImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public Order execute(String tenantId, String orderCode) {
        return orderRepository.findByCode(tenantId, orderCode)
                .orElseThrow(() -> new NotFoundException("Orden no encontrada: " + orderCode));
    }
}
