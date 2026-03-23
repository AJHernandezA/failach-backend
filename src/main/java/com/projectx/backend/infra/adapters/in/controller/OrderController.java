package com.projectx.backend.infra.adapters.in.controller;

import com.google.inject.Inject;
import com.projectx.backend.application.dto.CreateOrderRequest;
import com.projectx.backend.application.dto.UpdateStatusRequest;
import com.projectx.backend.domain.constants.ApiConstants;
import com.projectx.backend.domain.models.Order;
import com.projectx.backend.domain.models.OrderFilter;
import com.projectx.backend.domain.models.OrderStatus;
import com.projectx.backend.domain.models.Page;
import com.projectx.backend.domain.models.PaymentStatus;
import com.projectx.backend.domain.ports.in.CreateOrderUseCase;
import com.projectx.backend.domain.ports.in.GetOrderByCodeUseCase;
import com.projectx.backend.domain.ports.in.ListOrdersUseCase;
import com.projectx.backend.domain.ports.in.UpdateOrderStatusUseCase;
import io.javalin.Javalin;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Controller para los endpoints de órdenes.
 * POST usa sessionId cookie para identificar el carrito.
 * GET por orderCode es público.
 */
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);
    private static final String SESSION_COOKIE = "sessionId";

    private final CreateOrderUseCase createOrder;
    private final GetOrderByCodeUseCase getOrderByCode;
    private final UpdateOrderStatusUseCase updateOrderStatus;
    private final ListOrdersUseCase listOrders;

    @Inject
    public OrderController(CreateOrderUseCase createOrder,
                           GetOrderByCodeUseCase getOrderByCode,
                           UpdateOrderStatusUseCase updateOrderStatus,
                           ListOrdersUseCase listOrders) {
        this.createOrder = createOrder;
        this.getOrderByCode = getOrderByCode;
        this.updateOrderStatus = updateOrderStatus;
        this.listOrders = listOrders;
    }

    /**
     * Registra las rutas de órdenes en la instancia de Javalin.
     */
    public void register(Javalin app) {
        String base = ApiConstants.API_PREFIX + "/tenants/{tenantId}/orders";

        // POST /api/v1/tenants/:tenantId/orders — Crear orden desde carrito
        app.post(base, ctx -> {
            String tenantId = ctx.pathParam("tenantId");
            String sessionId = getSessionId(ctx);
            CreateOrderRequest request = ctx.bodyAsClass(CreateOrderRequest.class);
            Order order = createOrder.execute(tenantId, sessionId, request);
            ctx.status(201).json(Map.of("data", order));
        });

        // GET /api/v1/tenants/:tenantId/orders — Listar órdenes del tenant (paginado)
        app.get(base, ctx -> {
            String tenantId = ctx.pathParam("tenantId");
            String statusParam = ctx.queryParam("orderStatus");
            String paymentParam = ctx.queryParam("paymentStatus");
            int page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(0);
            int size = ctx.queryParamAsClass("size", Integer.class).getOrDefault(20);

            OrderStatus orderStatus = statusParam != null ? OrderStatus.valueOf(statusParam) : null;
            PaymentStatus paymentStatus = paymentParam != null ? PaymentStatus.valueOf(paymentParam) : null;

            Page<Order> result = listOrders.execute(tenantId, new OrderFilter(orderStatus, paymentStatus, page, size));
            ctx.json(Map.of("data", result.items(), "meta", Map.of(
                    "page", result.page(), "size", result.size(), "total", result.total())));
        });

        // GET /api/v1/tenants/:tenantId/orders/:orderCode — Obtener orden por código
        app.get(base + "/{orderCode}", ctx -> {
            String tenantId = ctx.pathParam("tenantId");
            String orderCode = ctx.pathParam("orderCode");
            Order order = getOrderByCode.execute(tenantId, orderCode);
            ctx.json(Map.of("data", order));
        });

        // PUT /api/v1/tenants/:tenantId/orders/:orderCode/status — Actualizar estado de orden
        app.put(base + "/{orderCode}/status", ctx -> {
            String tenantId = ctx.pathParam("tenantId");
            String orderCode = ctx.pathParam("orderCode");

            @SuppressWarnings("unchecked")
            Map<String, String> body = ctx.bodyAsClass(Map.class);
            OrderStatus newStatus = OrderStatus.valueOf(body.get("status"));
            String note = body.get("note");

            Order order = updateOrderStatus.execute(new UpdateStatusRequest(tenantId, orderCode, newStatus, note));
            ctx.json(Map.of("data", order));
        });

        log.info("Order endpoints registrados en {}/tenants/:tenantId/orders", ApiConstants.API_PREFIX);
    }

    /**
     * Obtiene el sessionId de la cookie. Requerido para crear orden.
     */
    private String getSessionId(Context ctx) {
        String sessionId = ctx.cookie(SESSION_COOKIE);
        if (sessionId == null || sessionId.isBlank()) {
            throw new com.projectx.backend.domain.exceptions.BadRequestException("Sesión no encontrada. Agrega productos al carrito primero.");
        }
        return sessionId;
    }
}
