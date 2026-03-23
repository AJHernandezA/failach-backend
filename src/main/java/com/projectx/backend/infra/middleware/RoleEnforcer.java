package com.projectx.backend.infra.middleware;

import com.projectx.backend.domain.exceptions.ForbiddenException;
import com.projectx.backend.domain.exceptions.UnauthorizedException;
import com.projectx.backend.domain.models.AuthenticatedUser;
import com.projectx.backend.domain.models.UserRole;
import io.javalin.http.Context;

/**
 * Utilidad para verificar roles y acceso a tenants en endpoints protegidos.
 * Se usa dentro de los controllers para verificar permisos.
 */
public final class RoleEnforcer {

    private RoleEnforcer() {}

    /**
     * Obtiene el usuario autenticado del contexto.
     * Lanza 401 si no hay usuario autenticado.
     */
    public static AuthenticatedUser getUser(Context ctx) {
        AuthenticatedUser user = ctx.attribute("user");
        if (user == null) {
            throw new UnauthorizedException("No autenticado");
        }
        return user;
    }

    /**
     * Verifica que el usuario tenga al menos uno de los roles dados.
     * Lanza 403 si no tiene ninguno.
     */
    public static AuthenticatedUser requireRole(Context ctx, UserRole... roles) {
        AuthenticatedUser user = getUser(ctx);
        if (!user.hasAnyRole(roles)) {
            throw new ForbiddenException("No tiene permisos para esta operación");
        }
        return user;
    }

    /**
     * Verifica que el usuario tenga uno de los roles dados Y acceso al tenant especificado.
     * SUPER_ADMIN siempre tiene acceso. MERCHANT solo a su propio tenant.
     * Lanza 403 si no tiene acceso.
     */
    public static AuthenticatedUser requireRoleAndTenant(Context ctx, String tenantId, UserRole... roles) {
        AuthenticatedUser user = requireRole(ctx, roles);
        if (!user.hasAccessToTenant(tenantId)) {
            throw new ForbiddenException("No tiene acceso al tenant: " + tenantId);
        }
        return user;
    }
}
