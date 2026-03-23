package com.projectx.backend.application.usecases;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.projectx.backend.domain.models.*;
import com.projectx.backend.domain.ports.in.GetDashboardMetricsUseCase;
import com.projectx.backend.domain.ports.out.OrderRepository;
import com.projectx.backend.domain.ports.out.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Implementación del caso de uso para obtener métricas del dashboard.
 * Calcula KPIs, pedidos recientes y productos más vendidos para un tenant.
 */
@Singleton
public class GetDashboardMetricsUseCaseImpl implements GetDashboardMetricsUseCase {

    private static final Logger log = LoggerFactory.getLogger(GetDashboardMetricsUseCaseImpl.class);

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @Inject
    public GetDashboardMetricsUseCaseImpl(OrderRepository orderRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    @Override
    public Map<String, Object> execute(String tenantId, String from, String to) {
        log.debug("Calculando métricas del dashboard para tenant: {}", tenantId);

        // Obtener todos los pedidos del tenant (sin filtro de estado para tener
        // métricas completas)
        OrderFilter filter = new OrderFilter(null, null, 0, 1000);
        Page<Order> ordersPage = orderRepository.findByTenant(tenantId, filter);
        List<Order> allOrders = ordersPage.items();

        // Filtrar solo pedidos no cancelados para métricas de revenue
        List<Order> activeOrders = allOrders.stream()
                .filter(o -> o.orderStatus() != OrderStatus.CANCELLED)
                .toList();

        // KPIs
        BigDecimal totalRevenue = activeOrders.stream()
                .map(Order::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalOrders = allOrders.size();
        int totalProducts = productRepository.countByTenantId(tenantId);

        BigDecimal avgTicket = activeOrders.isEmpty()
                ? BigDecimal.ZERO
                : totalRevenue.divide(BigDecimal.valueOf(activeOrders.size()), 0, RoundingMode.HALF_UP);

        // Pedidos recientes (últimos 10)
        List<Map<String, Object>> recentOrders = allOrders.stream()
                .sorted(Comparator.comparing(Order::createdAt).reversed())
                .limit(10)
                .map(o -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("orderCode", o.orderCode());
                    m.put("customerName", o.customer() != null ? o.customer().fullName() : "N/A");
                    m.put("amount", o.total());
                    m.put("status", o.orderStatus().name());
                    m.put("paymentMethod", o.paymentMethod() != null ? o.paymentMethod().name() : "N/A");
                    m.put("createdAt", o.createdAt().toString());
                    return m;
                })
                .toList();

        // Top productos vendidos (por cantidad)
        Map<String, Integer> productSales = new HashMap<>();
        Map<String, BigDecimal> productRevenue = new HashMap<>();
        Map<String, String> productNames = new HashMap<>();

        for (Order order : activeOrders) {
            if (order.items() == null)
                continue;
            for (OrderItem item : order.items()) {
                productSales.merge(item.productId(), item.quantity(), Integer::sum);
                productRevenue.merge(item.productId(),
                        item.price().multiply(BigDecimal.valueOf(item.quantity())),
                        BigDecimal::add);
                productNames.putIfAbsent(item.productId(), item.productName());
            }
        }

        List<Map<String, Object>> topProducts = productSales.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("productId", e.getKey());
                    m.put("name", productNames.getOrDefault(e.getKey(), "N/A"));
                    m.put("quantitySold", e.getValue());
                    m.put("revenue", productRevenue.getOrDefault(e.getKey(), BigDecimal.ZERO));
                    return m;
                })
                .toList();

        // Construir respuesta
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalRevenue", totalRevenue);
        result.put("totalOrders", totalOrders);
        result.put("totalProducts", totalProducts);
        result.put("avgTicket", avgTicket);
        result.put("revenueChange", 0);
        result.put("ordersChange", 0);
        result.put("recentOrders", recentOrders);
        result.put("topProducts", topProducts);

        return result;
    }
}
