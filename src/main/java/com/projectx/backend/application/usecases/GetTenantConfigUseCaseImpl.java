package com.projectx.backend.application.usecases;

import com.google.inject.Inject;
import com.projectx.backend.domain.exceptions.TenantInactiveException;
import com.projectx.backend.domain.exceptions.TenantNotFoundException;
import com.projectx.backend.domain.models.Tenant;
import com.projectx.backend.domain.ports.in.GetTenantConfigUseCase;
import com.projectx.backend.domain.ports.out.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementación del caso de uso para obtener la configuración de un tenant.
 * Busca el tenant en el repositorio, valida que exista y esté activo.
 */
public class GetTenantConfigUseCaseImpl implements GetTenantConfigUseCase {

    private static final Logger log = LoggerFactory.getLogger(GetTenantConfigUseCaseImpl.class);

    private final TenantRepository tenantRepository;

    @Inject
    public GetTenantConfigUseCaseImpl(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Override
    public Tenant execute(String tenantId) {
        log.debug("Buscando configuración del tenant: {}", tenantId);

        // Buscar tenant en el repositorio
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));

        // Validar que el tenant esté activo
        if (!tenant.isActive()) {
            throw new TenantInactiveException(tenantId);
        }

        log.debug("Configuración encontrada para tenant: {}", tenantId);
        return tenant;
    }
}
