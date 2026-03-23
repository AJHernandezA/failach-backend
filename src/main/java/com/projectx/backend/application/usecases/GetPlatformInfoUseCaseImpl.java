package com.projectx.backend.application.usecases;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.projectx.backend.domain.models.PlatformInfo;
import com.projectx.backend.domain.models.Tenant;
import com.projectx.backend.domain.ports.in.GetPlatformInfoUseCase;
import com.projectx.backend.domain.ports.out.PlatformRepository;
import com.projectx.backend.domain.ports.out.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;

/**
 * Implementación del caso de uso para obtener estadísticas públicas de la plataforma.
 * Cachea el resultado en memoria durante 5 minutos para evitar queries costosas.
 */
@Singleton
public class GetPlatformInfoUseCaseImpl implements GetPlatformInfoUseCase {

    private static final Logger log = LoggerFactory.getLogger(GetPlatformInfoUseCaseImpl.class);
    private static final long CACHE_TTL_SECONDS = 300; // 5 minutos

    private final TenantRepository tenantRepository;
    private final PlatformRepository platformRepository;

    // Cache simple en memoria
    private PlatformInfo cachedInfo;
    private Instant cacheExpiry = Instant.EPOCH;

    @Inject
    public GetPlatformInfoUseCaseImpl(TenantRepository tenantRepository, PlatformRepository platformRepository) {
        this.tenantRepository = tenantRepository;
        this.platformRepository = platformRepository;
    }

    @Override
    public PlatformInfo execute() {
        // Retornar del cache si no ha expirado
        if (cachedInfo != null && Instant.now().isBefore(cacheExpiry)) {
            return cachedInfo;
        }

        log.info("Calculando estadísticas de la plataforma...");

        // Contar tiendas activas
        List<Tenant> allTenants = tenantRepository.findAll();
        int activeStores = (int) allTenants.stream().filter(Tenant::isActive).count();

        // Contar productos y pedidos totales
        int totalProducts = platformRepository.countTotalProducts();
        int totalOrders = platformRepository.countTotalOrders();

        cachedInfo = new PlatformInfo(activeStores, totalProducts, totalOrders);
        cacheExpiry = Instant.now().plusSeconds(CACHE_TTL_SECONDS);

        log.info("Estadísticas calculadas: {} tiendas, {} productos, {} pedidos", activeStores, totalProducts, totalOrders);
        return cachedInfo;
    }
}
