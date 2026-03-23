package com.projectx.backend.infra.middleware;

import com.projectx.backend.domain.constants.ApiConstants;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

/**
 * Filtro multi-tenant que intercepta cada request y extrae el tenantId del
 * header X-Tenant-Id.
 * Rutas excluidas (como health check y webhooks) no requieren el header.
 * Si el header no está presente en una ruta que lo requiere, responde con 400
 * Bad Request.
 */
public class TenantFilter implements Handler {

    private static final Logger log = LoggerFactory.getLogger(TenantFilter.class);

    /** Rutas que no requieren el header X-Tenant-Id */
    private static final Set<String> EXCLUDED_PATHS = Set.of(
            ApiConstants.API_PREFIX + "/health");

    /** Prefijos de rutas excluidas del filtro */
    private static final Set<String> EXCLUDED_PREFIXES = Set.of(
            ApiConstants.API_PREFIX + "/webhooks/",
            ApiConstants.API_PREFIX + "/auth/",
            ApiConstants.API_PREFIX + "/platform/",
            ApiConstants.API_PREFIX + "/admin/",
            "/swagger",
            "/openapi");

    @Override
    public void handle(@NotNull Context ctx) throws Exception {
        // No aplicar filtro a preflight CORS (OPTIONS no envía headers custom)
        if ("OPTIONS".equalsIgnoreCase(ctx.method().name())) {
            return;
        }

        String path = ctx.path();

        // No aplicar filtro a rutas excluidas
        if (isExcluded(path)) {
            return;
        }

        String tenantId = ctx.header(ApiConstants.TENANT_HEADER);

        if (tenantId == null || tenantId.isBlank()) {
            log.warn("Request sin header X-Tenant-Id: {} {}", ctx.method(), path);
            ctx.status(400).json(Map.of(
                    "error", Map.of(
                            "code", "MISSING_TENANT",
                            "message", "X-Tenant-Id header is required")));
            ctx.skipRemainingHandlers();
            return;
        }

        // Normalizar a minúsculas para consistencia
        tenantId = tenantId.trim().toLowerCase();

        // Inyectar tenantId en el contexto para uso en controllers y handlers
        ctx.attribute(ApiConstants.TENANT_ATTRIBUTE, tenantId);
        log.debug("Tenant identificado: {}", tenantId);
    }

    /**
     * Verifica si la ruta está excluida del filtro de tenant.
     */
    private boolean isExcluded(String path) {
        if (EXCLUDED_PATHS.contains(path)) {
            return true;
        }
        for (String prefix : EXCLUDED_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
