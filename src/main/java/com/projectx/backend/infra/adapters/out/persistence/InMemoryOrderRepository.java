package com.projectx.backend.infra.adapters.out.persistence;

import com.projectx.backend.domain.models.Order;
import com.projectx.backend.domain.models.OrderFilter;
import com.projectx.backend.domain.models.Page;
import com.projectx.backend.domain.ports.out.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Implementación en memoria del repositorio de órdenes.
 * Para desarrollo sin DynamoDB Local.
 */
public class InMemoryOrderRepository implements OrderRepository {

    private static final Logger log = LoggerFactory.getLogger(InMemoryOrderRepository.class);

    /** Key: tenantId::orderId */
    private final Map<String, Order> store = new ConcurrentHashMap<>();

    /** Índice secundario: tenantId::orderCode → orderId */
    private final Map<String, String> codeIndex = new ConcurrentHashMap<>();

    private String key(String tenantId, String orderId) {
        return tenantId + "::" + orderId;
    }

    private String codeKey(String tenantId, String orderCode) {
        return tenantId + "::" + orderCode;
    }

    @Override
    public void save(Order order) {
        log.debug("[InMemory] Guardando orden: {} ({})", order.orderId(), order.orderCode());
        store.put(key(order.tenantId(), order.orderId()), order);
        codeIndex.put(codeKey(order.tenantId(), order.orderCode()), order.orderId());
    }

    @Override
    public Optional<Order> findByCode(String tenantId, String orderCode) {
        log.debug("[InMemory] Buscando orden por código: {}:{}", tenantId, orderCode);
        String orderId = codeIndex.get(codeKey(tenantId, orderCode));
        if (orderId == null) return Optional.empty();
        return Optional.ofNullable(store.get(key(tenantId, orderId)));
    }

    @Override
    public Page<Order> findByTenant(String tenantId, OrderFilter filter) {
        log.debug("[InMemory] Listando órdenes de tenant: {}", tenantId);
        List<Order> all = store.values().stream()
                .filter(o -> o.tenantId().equals(tenantId))
                .filter(o -> filter.orderStatus() == null || o.orderStatus() == filter.orderStatus())
                .filter(o -> filter.paymentStatus() == null || o.paymentStatus() == filter.paymentStatus())
                .sorted((a, b) -> b.createdAt().compareTo(a.createdAt()))
                .collect(Collectors.toList());

        int total = all.size();
        int from = filter.page() * filter.size();
        int to = Math.min(from + filter.size(), total);

        List<Order> items = from < total ? all.subList(from, to) : List.of();
        return new Page<>(items, filter.page(), filter.size(), total);
    }

    @Override
    public void update(Order order) {
        log.debug("[InMemory] Actualizando orden: {}", order.orderId());
        store.put(key(order.tenantId(), order.orderId()), order);
    }
}
