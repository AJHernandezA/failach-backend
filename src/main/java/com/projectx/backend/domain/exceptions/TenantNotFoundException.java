package com.projectx.backend.domain.exceptions;

/**
 * Excepción cuando el tenant solicitado no existe en el sistema (HTTP 404).
 */
public class TenantNotFoundException extends NotFoundException {

    public TenantNotFoundException(String tenantId) {
        super("Tenant no encontrado: " + tenantId);
    }
}
