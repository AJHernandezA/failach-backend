package com.projectx.backend.domain.ports.in;

import com.projectx.backend.domain.models.Tenant;

/**
 * Puerto de entrada para obtener la configuración de un tenant.
 */
public interface GetTenantConfigUseCase {

    /**
     * Obtiene la configuración completa de un tenant.
     *
     * @param tenantId identificador del tenant
     * @return configuración del tenant
     * @throws com.projectx.backend.domain.exceptions.TenantNotFoundException si no existe
     * @throws com.projectx.backend.domain.exceptions.TenantInactiveException si está inactivo
     */
    Tenant execute(String tenantId);
}
