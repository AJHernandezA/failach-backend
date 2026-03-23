package com.projectx.backend.domain.ports.in;

import com.projectx.backend.domain.models.Tenant;
import com.projectx.backend.application.dto.UpdateTenantRequest;

/**
 * Puerto de entrada para actualizar la configuración de un tenant.
 */
public interface UpdateTenantConfigUseCase {

    /**
     * Actualiza la configuración de un tenant existente.
     *
     * @param tenantId identificador del tenant
     * @param request  datos a actualizar
     * @return tenant actualizado
     * @throws com.projectx.backend.domain.exceptions.TenantNotFoundException si no existe
     * @throws com.projectx.backend.domain.exceptions.BadRequestException si la validación falla
     */
    Tenant execute(String tenantId, UpdateTenantRequest request);
}
