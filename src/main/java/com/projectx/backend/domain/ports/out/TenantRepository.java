package com.projectx.backend.domain.ports.out;

import com.projectx.backend.domain.models.Tenant;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida para persistencia de tenants.
 * La implementación concreta se encuentra en la capa de infraestructura.
 */
public interface TenantRepository {

    /**
     * Busca un tenant por su identificador único.
     *
     * @param tenantId identificador del tenant (slug)
     * @return Optional con el tenant si existe
     */
    Optional<Tenant> findById(String tenantId);

    /**
     * Guarda o actualiza un tenant.
     *
     * @param tenant entidad a persistir
     */
    void save(Tenant tenant);

    /**
     * Obtiene todos los tenants registrados.
     * Solo para uso administrativo (SUPER_ADMIN).
     *
     * @return lista de todos los tenants
     */
    List<Tenant> findAll();
}
