package com.projectx.backend.infra.adapters.in.controller;

import com.google.inject.Inject;
import com.projectx.backend.domain.constants.ApiConstants;
import com.projectx.backend.domain.models.PaymentInitData;
import com.projectx.backend.domain.models.PaymentStatusResponse;
import com.projectx.backend.domain.models.WompiWebhookEvent;
import com.projectx.backend.domain.ports.in.GetPaymentStatusUseCase;
import com.projectx.backend.domain.ports.in.InitiateWompiPaymentUseCase;
import com.projectx.backend.domain.ports.in.ProcessWompiWebhookUseCase;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Controller para los endpoints de integración con Wompi.
 */
public class WompiController {

    private static final Logger log = LoggerFactory.getLogger(WompiController.class);

    private final InitiateWompiPaymentUseCase initiatePayment;
    private final ProcessWompiWebhookUseCase processWebhook;
    private final GetPaymentStatusUseCase getPaymentStatus;

    @Inject
    public WompiController(InitiateWompiPaymentUseCase initiatePayment,
            ProcessWompiWebhookUseCase processWebhook,
            GetPaymentStatusUseCase getPaymentStatus) {
        this.initiatePayment = initiatePayment;
        this.processWebhook = processWebhook;
        this.getPaymentStatus = getPaymentStatus;
    }

    /**
     * Registra las rutas de Wompi en la instancia de Javalin.
     */
    public void register(Javalin app) {
        String tenantBase = ApiConstants.API_PREFIX + "/tenants/{tenantId}";

        // POST /api/v1/tenants/:tenantId/payments/wompi/init — Iniciar pago
        app.post(tenantBase + "/payments/wompi/init", ctx -> {
            String tenantId = ctx.pathParam("tenantId");

            @SuppressWarnings("unchecked")
            Map<String, String> body = ctx.bodyAsClass(Map.class);
            String orderCode = body.get("orderCode");
            String redirectUrl = body.get("redirectUrl");

            PaymentInitData data = initiatePayment.execute(tenantId, orderCode, redirectUrl);
            ctx.json(Map.of("data", data));
        });

        // POST /api/v1/webhooks/wompi — Webhook de Wompi
        app.post(ApiConstants.API_PREFIX + "/webhooks/wompi", ctx -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = ctx.bodyAsClass(Map.class);

            // Parsear el evento del webhook
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) body.get("data");
            @SuppressWarnings("unchecked")
            Map<String, Object> transaction = (Map<String, Object>) data.get("transaction");

            @SuppressWarnings("unchecked")
            Map<String, String> signatureMap = body.containsKey("signature")
                    ? (Map<String, String>) body.get("signature")
                    : Map.of();
            String signature = signatureMap.getOrDefault("checksum", "");

            WompiWebhookEvent event = new WompiWebhookEvent(
                    (String) body.get("event"),
                    signature,
                    body.containsKey("timestamp") ? ((Number) body.get("timestamp")).longValue() : 0L,
                    (String) transaction.get("id"),
                    (String) transaction.get("status"),
                    (String) transaction.get("reference"),
                    ((Number) transaction.get("amount_in_cents")).longValue());

            processWebhook.execute(event);
            ctx.status(200).json(Map.of("status", "ok"));
        });

        // GET /api/v1/tenants/:tenantId/orders/:orderCode/payment-status
        app.get(tenantBase + "/orders/{orderCode}/payment-status", ctx -> {
            String tenantId = ctx.pathParam("tenantId");
            String orderCode = ctx.pathParam("orderCode");
            PaymentStatusResponse status = getPaymentStatus.execute(tenantId, orderCode);
            ctx.json(Map.of("data", status));
        });

        log.info("Wompi endpoints registrados");
    }
}
