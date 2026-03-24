package com.projectx.backend.infra.adapters.in.controller;

import com.google.inject.Inject;
import com.projectx.backend.application.dto.UpdateTenantRequest;
import com.projectx.backend.domain.constants.ApiConstants;
import com.projectx.backend.domain.models.Tenant;
import com.projectx.backend.domain.models.UserRole;
import com.projectx.backend.domain.ports.in.GetTenantConfigUseCase;
import com.projectx.backend.domain.ports.in.UpdateTenantConfigUseCase;
import com.projectx.backend.domain.ports.out.AdminNotificationService;
import com.projectx.backend.infra.middleware.RoleEnforcer;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Controller para los endpoints de configuración de tenant.
 * Registra las rutas GET y PUT para obtener y actualizar la config.
 */
public class TenantController {

    private static final Logger log = LoggerFactory.getLogger(TenantController.class);

    private final GetTenantConfigUseCase getTenantConfig;
    private final UpdateTenantConfigUseCase updateTenantConfig;
    private final AdminNotificationService adminNotificationService;

    @Inject
    public TenantController(GetTenantConfigUseCase getTenantConfig,
            UpdateTenantConfigUseCase updateTenantConfig,
            AdminNotificationService adminNotificationService) {
        this.getTenantConfig = getTenantConfig;
        this.updateTenantConfig = updateTenantConfig;
        this.adminNotificationService = adminNotificationService;
    }

    /**
     * Registra las rutas de tenant en la instancia de Javalin.
     */
    public void register(Javalin app) {
        // GET /api/v1/tenants/:tenantId/config — Obtener configuración pública
        app.get(ApiConstants.API_PREFIX + "/tenants/{tenantId}/config", ctx -> {
            String tenantId = ctx.pathParam("tenantId");
            Tenant tenant = getTenantConfig.execute(tenantId);
            ctx.json(Map.of("data", tenant));
        });

        // PUT /api/v1/tenants/:tenantId/config — Actualizar configuración (requiere
        // SUPER_ADMIN o MERCHANT del tenant)
        app.put(ApiConstants.API_PREFIX + "/tenants/{tenantId}/config", ctx -> {
            String tenantId = ctx.pathParam("tenantId");
            RoleEnforcer.requireRoleAndTenant(ctx, tenantId, UserRole.SUPER_ADMIN, UserRole.MERCHANT);
            UpdateTenantRequest request = ctx.bodyAsClass(UpdateTenantRequest.class);
            Tenant updated = updateTenantConfig.execute(tenantId, request);

            // F045: Notificar al admin sobre tienda actualizada
            try {
                adminNotificationService.notifyStoreUpdated(tenantId, updated.name());
            } catch (Exception e) {
                log.warn("Error al notificar al admin sobre tienda actualizada: {}", e.getMessage());
            }

            ctx.json(Map.of("data", updated));
        });

        log.info("Tenant endpoints registrados: GET/PUT {}/tenants/:tenantId/config", ApiConstants.API_PREFIX);
    }
}
