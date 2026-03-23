package com.projectx.backend.infra.adapters.out.persistence;

import com.projectx.backend.domain.models.Tenant;
import com.projectx.backend.domain.ports.out.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementación en memoria del repositorio de tenants.
 * Se usa en desarrollo cuando DynamoDB Local no está disponible.
 * Los datos se pierden al reiniciar la aplicación.
 */
public class InMemoryTenantRepository implements TenantRepository {

    private static final Logger log = LoggerFactory.getLogger(InMemoryTenantRepository.class);

    private final Map<String, Tenant> store = new ConcurrentHashMap<>();

    @Override
    public Optional<Tenant> findById(String tenantId) {
        log.debug("[InMemory] Buscando tenant: {}", tenantId);
        return Optional.ofNullable(store.get(tenantId));
    }

    @Override
    public void save(Tenant tenant) {
        log.debug("[InMemory] Guardando tenant: {}", tenant.tenantId());
        store.put(tenant.tenantId(), tenant);
    }

    @Override
    public List<Tenant> findAll() {
        return new ArrayList<>(store.values());
    }
}
