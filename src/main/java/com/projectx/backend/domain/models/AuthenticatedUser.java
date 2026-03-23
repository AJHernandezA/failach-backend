package com.projectx.backend.domain.models;

/**
 * Representa un usuario autenticado extraído del JWT.
 *
 * @param sub      identificador único del usuario (Cognito sub)
 * @param email    email del usuario
 * @param role     rol del usuario en el sistema
 * @param tenantId tenant asociado al usuario (null para SUPER_ADMIN)
 */
public record AuthenticatedUser(
        String sub,
        String email,
        UserRole role,
        String tenantId
) {

    /**
     * Verifica si el usuario tiene acceso al tenant dado.
     * SUPER_ADMIN tiene acceso a todos los tenants.
     * MERCHANT solo tiene acceso a su propio tenant.
     */
    public boolean hasAccessToTenant(String targetTenantId) {
        if (role == UserRole.SUPER_ADMIN) {
            return true;
        }
        return tenantId != null && tenantId.equals(targetTenantId);
    }

    /**
     * Verifica si el usuario tiene alguno de los roles dados.
     */
    public boolean hasAnyRole(UserRole... roles) {
        for (UserRole r : roles) {
            if (this.role == r) {
                return true;
            }
        }
        return false;
    }
}
