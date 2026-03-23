package com.projectx.backend.domain.ports.out;

import com.projectx.backend.domain.models.Order;
import com.projectx.backend.domain.models.OrderFilter;
import com.projectx.backend.domain.models.Page;

import java.util.Optional;

/**
 * Puerto de salida para persistencia de órdenes.
 */
public interface OrderRepository {

    void save(Order order);

    Optional<Order> findByCode(String tenantId, String orderCode);

    Page<Order> findByTenant(String tenantId, OrderFilter filter);

    void update(Order order);
}
