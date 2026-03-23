package com.projectx.backend.domain.ports.in;

import com.projectx.backend.domain.models.Order;
import com.projectx.backend.domain.models.OrderFilter;
import com.projectx.backend.domain.models.Page;

/**
 * Lista las órdenes de un tenant con paginación y filtros.
 */
public interface ListOrdersUseCase {
    Page<Order> execute(String tenantId, OrderFilter filter);
}
