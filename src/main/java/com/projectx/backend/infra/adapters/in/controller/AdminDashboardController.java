package com.projectx.backend.infra.adapters.in.controller;

import com.google.inject.Inject;
import com.projectx.backend.domain.constants.ApiConstants;
import com.projectx.backend.domain.models.AuthenticatedUser;
import com.projectx.backend.domain.models.UserRole;
import com.projectx.backend.domain.ports.in.GetDashboardMetricsUseCase;
import com.projectx.backend.infra.middleware.RoleEnforcer;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Controller para el endpoint de métricas del dashboard de administración.
 * Requiere autenticación. MERCHANT solo ve métricas de su tenant.
 * SUPER_ADMIN puede filtrar por tenant o ver consolidado.
 */
public class AdminDashboardController {

    private static final Logger log = LoggerFactory.getLogger(AdminDashboardController.class);

    private final GetDashboardMetricsUseCase getDashboardMetrics;

    @Inject
    public AdminDashboardController(GetDashboardMetricsUseCase getDashboardMetrics) {
        this.getDashboardMetrics = getDashboardMetrics;
    }

    /**
     * Registra las rutas del dashboard admin en Javalin.
     */
    public void register(Javalin app) {
        // GET /api/v1/admin/dashboard — Métricas agregadas del dashboard
        app.get(ApiConstants.API_PREFIX + "/admin/dashboard", ctx -> {
            AuthenticatedUser user = RoleEnforcer.requireRole(ctx, UserRole.SUPER_ADMIN, UserRole.MERCHANT);

            String tenantId;
            if (user.role() == UserRole.SUPER_ADMIN) {
                tenantId = ctx.queryParam("tenantId");
                if (tenantId == null || tenantId.isBlank()) {
                    tenantId = "idoneo"; // Default para SUPER_ADMIN sin filtro
                }
            } else {
                tenantId = user.tenantId();
            }

            String from = ctx.queryParam("from");
            String to = ctx.queryParam("to");

            Map<String, Object> metrics = getDashboardMetrics.execute(tenantId, from, to);
            ctx.json(Map.of("data", metrics));
        });

        log.info("Admin Dashboard endpoint registrado: GET {}/admin/dashboard", ApiConstants.API_PREFIX);
    }
}
