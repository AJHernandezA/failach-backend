package com.projectx.backend.infra.adapters.in.controller;

import com.google.inject.Inject;
import com.projectx.backend.application.dto.UpdateStatusRequest;
import com.projectx.backend.domain.constants.ApiConstants;
import com.projectx.backend.domain.models.*;
import com.projectx.backend.domain.ports.in.*;
import com.projectx.backend.infra.middleware.RoleEnforcer;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Controller para la gestión de pedidos desde el panel de administración.
 * Todos los endpoints requieren autenticación (MERCHANT o SUPER_ADMIN).
 * El tenantId se resuelve automáticamente del usuario autenticado.
 */
public class AdminOrderController {

    private static final Logger log = LoggerFactory.getLogger(AdminOrderController.class);

    private final ListOrdersUseCase listOrders;
    private final GetOrderByCodeUseCase getOrderByCode;
    private final UpdateOrderStatusUseCase updateOrderStatus;
    private final CancelOrderUseCase cancelOrder;
    private final CreateOrderUseCase createOrder;

    @Inject
    public AdminOrderController(
            ListOrdersUseCase listOrders,
            GetOrderByCodeUseCase getOrderByCode,
            UpdateOrderStatusUseCase updateOrderStatus,
            CancelOrderUseCase cancelOrder,
            CreateOrderUseCase createOrder) {
        this.listOrders = listOrders;
        this.getOrderByCode = getOrderByCode;
        this.updateOrderStatus = updateOrderStatus;
        this.cancelOrder = cancelOrder;
        this.createOrder = createOrder;
    }

    /**
     * Registra las rutas admin de pedidos en Javalin.
     */
    public void register(Javalin app) {
        String base = ApiConstants.API_PREFIX + "/admin/orders";

        // GET /api/v1/admin/orders — Listar pedidos del tenant autenticado
        app.get(base, ctx -> {
            AuthenticatedUser user = RoleEnforcer.requireRole(ctx, UserRole.SUPER_ADMIN, UserRole.MERCHANT);
            String tenantId = resolveTenantId(user, ctx.queryParam("tenantId"));

            String statusParam = ctx.queryParam("status");
            String paymentParam = ctx.queryParam("paymentStatus");
            int page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(0);
            int size = ctx.queryParamAsClass("size", Integer.class).getOrDefault(20);

            OrderStatus orderStatus = null;
            if (statusParam != null && !statusParam.isBlank()) {
                try { orderStatus = OrderStatus.valueOf(statusParam.toUpperCase()); } catch (IllegalArgumentException ignored) {}
            }
            PaymentStatus paymentStatus = null;
            if (paymentParam != null && !paymentParam.isBlank()) {
                try { paymentStatus = PaymentStatus.valueOf(paymentParam.toUpperCase()); } catch (IllegalArgumentException ignored) {}
            }

            OrderFilter filter = new OrderFilter(orderStatus, paymentStatus, page, size);
            Page<Order> result = listOrders.execute(tenantId, filter);

            ctx.json(Map.of(
                    "data", result.items(),
                    "meta", Map.of("page", result.page(), "size", result.size(), "total", result.total())
            ));
        });

        // GET /api/v1/admin/orders/:code — Detalle completo de un pedido
        app.get(base + "/{orderCode}", ctx -> {
            AuthenticatedUser user = RoleEnforcer.requireRole(ctx, UserRole.SUPER_ADMIN, UserRole.MERCHANT);
            String tenantId = resolveTenantId(user, ctx.queryParam("tenantId"));
            String orderCode = ctx.pathParam("orderCode");

            Order order = getOrderByCode.execute(tenantId, orderCode);
            ctx.json(Map.of("data", order));
        });

        // PUT /api/v1/admin/orders/:code/status — Actualizar estado del pedido
        app.put(base + "/{orderCode}/status", ctx -> {
            AuthenticatedUser user = RoleEnforcer.requireRole(ctx, UserRole.SUPER_ADMIN, UserRole.MERCHANT);
            String tenantId = resolveTenantId(user, ctx.queryParam("tenantId"));
            String orderCode = ctx.pathParam("orderCode");

            @SuppressWarnings("unchecked")
            Map<String, String> body = ctx.bodyAsClass(Map.class);
            OrderStatus newStatus = OrderStatus.valueOf(body.get("status"));
            String note = body.getOrDefault("note", null);

            Order order = updateOrderStatus.execute(new UpdateStatusRequest(tenantId, orderCode, newStatus, note));
            ctx.json(Map.of("data", order));
        });

        // PUT /api/v1/admin/orders/:code/cancel — Cancelar pedido con motivo
        app.put(base + "/{orderCode}/cancel", ctx -> {
            AuthenticatedUser user = RoleEnforcer.requireRole(ctx, UserRole.SUPER_ADMIN, UserRole.MERCHANT);
            String tenantId = resolveTenantId(user, ctx.queryParam("tenantId"));
            String orderCode = ctx.pathParam("orderCode");

            @SuppressWarnings("unchecked")
            Map<String, String> body = ctx.bodyAsClass(Map.class);
            String reason = body.getOrDefault("reason", "Cancelado por el vendedor");

            Order order = cancelOrder.execute(tenantId, orderCode, reason);
            ctx.json(Map.of("data", order));
        });

        log.info("Admin Order endpoints registrados: {}/admin/orders", ApiConstants.API_PREFIX);
    }

    /**
     * Resuelve el tenantId: MERCHANT usa su propio tenant, SUPER_ADMIN puede elegir.
     */
    private String resolveTenantId(AuthenticatedUser user, String requestedTenantId) {
        if (user.role() == UserRole.SUPER_ADMIN && requestedTenantId != null && !requestedTenantId.isBlank()) {
            return requestedTenantId;
        }
        return user.tenantId();
    }
}
