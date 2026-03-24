package com.projectx.backend.infra.adapters.in.controller;

import com.google.inject.Inject;
import com.projectx.backend.domain.constants.ApiConstants;
import com.projectx.backend.domain.models.PaymentLink;
import com.projectx.backend.domain.ports.out.PaymentService;
import com.projectx.backend.infra.middleware.RoleEnforcer;
import com.projectx.backend.domain.models.UserRole;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Controller para la gestión de links de pago de Wompi desde el admin.
 * Requiere autenticación (MERCHANT o SUPER_ADMIN).
 */
public class AdminPaymentLinkController {

    private static final Logger log = LoggerFactory.getLogger(AdminPaymentLinkController.class);

    private final PaymentService paymentService;

    @Inject
    public AdminPaymentLinkController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Registra las rutas de links de pago en Javalin.
     */
    public void register(Javalin app) {
        String base = ApiConstants.API_PREFIX + "/admin/payment-links";

        // GET /api/v1/admin/payment-links — Listar links de pago
        app.get(base, ctx -> {
            RoleEnforcer.requireRole(ctx, UserRole.SUPER_ADMIN, UserRole.MERCHANT);

            List<PaymentLink> links = paymentService.listPaymentLinks();
            ctx.json(Map.of("data", links));
        });

        // GET /api/v1/admin/payment-links/:id — Obtener un link de pago
        app.get(base + "/{id}", ctx -> {
            RoleEnforcer.requireRole(ctx, UserRole.SUPER_ADMIN, UserRole.MERCHANT);

            String id = ctx.pathParam("id");
            PaymentLink link = paymentService.getPaymentLink(id);
            if (link == null) {
                ctx.status(404).json(Map.of("error", Map.of("message", "Link de pago no encontrado")));
                return;
            }
            ctx.json(Map.of("data", link));
        });

        // POST /api/v1/admin/payment-links — Crear link de pago
        app.post(base, ctx -> {
            RoleEnforcer.requireRole(ctx, UserRole.SUPER_ADMIN, UserRole.MERCHANT);

            @SuppressWarnings("unchecked")
            Map<String, Object> body = ctx.bodyAsClass(Map.class);

            String name = (String) body.get("name");
            String description = (String) body.getOrDefault("description", "");
            boolean singleUse = Boolean.TRUE.equals(body.get("singleUse"));
            Long amountInCents = body.get("amountInCents") != null
                    ? ((Number) body.get("amountInCents")).longValue() : null;
            String imageUrl = (String) body.get("imageUrl");
            String redirectUrl = (String) body.get("redirectUrl");

            if (name == null || name.isBlank()) {
                ctx.status(400).json(Map.of("error", Map.of("message", "El nombre es obligatorio")));
                return;
            }

            PaymentLink link = paymentService.createPaymentLink(
                    name, description, singleUse, amountInCents, imageUrl, redirectUrl);

            log.info("Link de pago creado: id={}, name={}", link.id(), link.name());
            ctx.status(201).json(Map.of("data", link));
        });

        log.info("Admin Payment Links endpoints registrados: {}", base);
    }
}
