package com.projectx.backend.domain.exceptions;

/**
 * Excepción cuando el tenant existe pero está inactivo (HTTP 403).
 */
public class TenantInactiveException extends ForbiddenException {

    public TenantInactiveException(String tenantId) {
        super("Tienda no disponible: " + tenantId);
    }
}
