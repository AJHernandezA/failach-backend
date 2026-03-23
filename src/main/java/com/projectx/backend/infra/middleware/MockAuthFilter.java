package com.projectx.backend.infra.middleware;

import com.projectx.backend.domain.models.AuthenticatedUser;
import com.projectx.backend.domain.models.UserRole;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.UnauthorizedResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filtro de autenticación mock para desarrollo.
 * En lugar de validar JWT contra Cognito, lee headers mock:
 * - X-Mock-Role: SUPER_ADMIN | MERCHANT | BUYER
 * - X-Mock-Email: email del usuario simulado
 * - X-Mock-TenantId: tenantId del usuario (para MERCHANT)
 *
 * Se activa solo cuando auth.mock=true en application.properties.
 * Si no se envía X-Mock-Role, retorna 401.
 */
public class MockAuthFilter implements Handler {

    private static final Logger log = LoggerFactory.getLogger(MockAuthFilter.class);

    @Override
    public void handle(Context ctx) throws Exception {
        String mockRole = ctx.header("X-Mock-Role");

        if (mockRole == null || mockRole.isBlank()) {
            throw new UnauthorizedResponse("Token de autenticación requerido (mock: enviar X-Mock-Role)");
        }

        UserRole role;
        try {
            role = UserRole.valueOf(mockRole.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new UnauthorizedResponse("Rol mock inválido: " + mockRole);
        }

        String email = ctx.header("X-Mock-Email");
        if (email == null) email = "mock-" + role.name().toLowerCase() + "@projectx.dev";

        String tenantId = ctx.header("X-Mock-TenantId");
        if (tenantId == null && role == UserRole.MERCHANT) {
            // Si es MERCHANT y no envía tenantId, usar el del header X-Tenant-Id
            tenantId = ctx.header("X-Tenant-Id");
        }

        String sub = "mock-" + role.name().toLowerCase() + "-" + (tenantId != null ? tenantId : "global");

        AuthenticatedUser user = new AuthenticatedUser(sub, email, role, tenantId);
        ctx.attribute("user", user);

        log.debug("[MockAuth] Usuario autenticado: {} ({}), rol: {}, tenant: {}",
                email, sub, role, tenantId);
    }
}
