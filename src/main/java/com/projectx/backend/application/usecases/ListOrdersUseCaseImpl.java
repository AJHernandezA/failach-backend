package com.projectx.backend.application.usecases;

import com.google.inject.Inject;
import com.projectx.backend.domain.models.Order;
import com.projectx.backend.domain.models.OrderFilter;
import com.projectx.backend.domain.models.Page;
import com.projectx.backend.domain.ports.in.ListOrdersUseCase;
import com.projectx.backend.domain.ports.out.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lista las órdenes de un tenant con paginación y filtros opcionales.
 */
public class ListOrdersUseCaseImpl implements ListOrdersUseCase {

    private static final Logger log = LoggerFactory.getLogger(ListOrdersUseCaseImpl.class);

    private final OrderRepository orderRepository;

    @Inject
    public ListOrdersUseCaseImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public Page<Order> execute(String tenantId, OrderFilter filter) {
        log.debug("Listando órdenes de tenant {} con filtros: {}", tenantId, filter);
        return orderRepository.findByTenant(tenantId, filter);
    }
}
