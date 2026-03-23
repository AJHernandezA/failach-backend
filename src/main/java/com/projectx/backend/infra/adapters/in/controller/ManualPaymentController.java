package com.projectx.backend.infra.adapters.in.controller;

import com.google.inject.Inject;
import com.projectx.backend.domain.constants.ApiConstants;
import com.projectx.backend.domain.models.Order;
import com.projectx.backend.domain.ports.in.CancelOrderUseCase;
import com.projectx.backend.domain.ports.in.ConfirmManualPaymentUseCase;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Controller para endpoints de pago manual y cancelación de órdenes.
 */
public class ManualPaymentController {

    private static final Logger log = LoggerFactory.getLogger(ManualPaymentController.class);

    private final ConfirmManualPaymentUseCase confirmPayment;
    private final CancelOrderUseCase cancelOrder;

    @Inject
    public ManualPaymentController(ConfirmManualPaymentUseCase confirmPayment,
                                    CancelOrderUseCase cancelOrder) {
        this.confirmPayment = confirmPayment;
        this.cancelOrder = cancelOrder;
    }

    /**
     * Registra las rutas de pago manual en la instancia de Javalin.
     */
    public void register(Javalin app) {
        String tenantBase = ApiConstants.API_PREFIX + "/tenants/{tenantId}";

        // PUT /api/v1/tenants/:tenantId/orders/:orderCode/confirm-payment
        app.put(tenantBase + "/orders/{orderCode}/confirm-payment", ctx -> {
            String tenantId = ctx.pathParam("tenantId");
            String orderCode = ctx.pathParam("orderCode");

            @SuppressWarnings("unchecked")
            Map<String, String> body = ctx.bodyAsClass(Map.class);
            String note = body != null ? body.get("note") : null;

            Order order = confirmPayment.execute(tenantId, orderCode, note);
            ctx.json(Map.of("data", order));
        });

        // PUT /api/v1/tenants/:tenantId/orders/:orderCode/cancel
        app.put(tenantBase + "/orders/{orderCode}/cancel", ctx -> {
            String tenantId = ctx.pathParam("tenantId");
            String orderCode = ctx.pathParam("orderCode");

            @SuppressWarnings("unchecked")
            Map<String, String> body = ctx.bodyAsClass(Map.class);
            String reason = body != null ? body.get("reason") : null;

            Order order = cancelOrder.execute(tenantId, orderCode, reason);
            ctx.json(Map.of("data", order));
        });

        log.info("Manual payment endpoints registrados");
    }
}
