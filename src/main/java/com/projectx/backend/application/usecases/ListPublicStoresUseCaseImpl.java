package com.projectx.backend.application.usecases;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.projectx.backend.domain.models.StorePreview;
import com.projectx.backend.domain.models.Tenant;
import com.projectx.backend.domain.ports.in.ListPublicStoresUseCase;
import com.projectx.backend.domain.ports.out.ProductRepository;
import com.projectx.backend.domain.ports.out.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementación del caso de uso para listar tiendas activas de la plataforma.
 * Filtra por categoría, búsqueda y ordena según el criterio solicitado.
 * Solo retorna tiendas con isActive=true.
 */
@Singleton
public class ListPublicStoresUseCaseImpl implements ListPublicStoresUseCase {

    private static final Logger log = LoggerFactory.getLogger(ListPublicStoresUseCaseImpl.class);

    private final TenantRepository tenantRepository;
    private final ProductRepository productRepository;

    @Inject
    public ListPublicStoresUseCaseImpl(TenantRepository tenantRepository, ProductRepository productRepository) {
        this.tenantRepository = tenantRepository;
        this.productRepository = productRepository;
    }

    @Override
    public List<StorePreview> execute(String category, String search, String sort) {
        List<Tenant> allTenants = tenantRepository.findAll();

        // Filtrar solo tiendas activas
        var filtered = allTenants.stream()
                .filter(Tenant::isActive);

        // Filtrar por búsqueda en nombre (case-insensitive)
        if (search != null && !search.isBlank()) {
            String lowerSearch = search.toLowerCase().trim();
            filtered = filtered.filter(t ->
                    t.name().toLowerCase().contains(lowerSearch) ||
                    (t.description() != null && t.description().toLowerCase().contains(lowerSearch))
            );
        }

        // Mapear a StorePreview con conteo de productos
        List<StorePreview> previews = filtered.map(t -> {
            int productCount = productRepository.countByTenantId(t.tenantId());
            String storeUrl = "https://" + t.tenantId() + ".projectx.com";
            // Determinar categoría del negocio basándose en la descripción o nombre
            String businessCategory = inferCategory(t);

            return new StorePreview(
                    t.tenantId(),
                    t.name(),
                    businessCategory,
                    t.description() != null ? t.description() : "",
                    t.logoUrl() != null ? t.logoUrl() : "",
                    storeUrl,
                    productCount
            );
        }).collect(Collectors.toList());

        // Filtrar por categoría si se proporcionó
        if (category != null && !category.isBlank()) {
            String lowerCategory = category.toLowerCase().trim();
            previews = previews.stream()
                    .filter(p -> p.category().toLowerCase().contains(lowerCategory))
                    .collect(Collectors.toList());
        }

        // Ordenar
        if ("popular".equalsIgnoreCase(sort)) {
            previews.sort(Comparator.comparingInt(StorePreview::totalProducts).reversed());
        } else {
            // Por defecto: más recientes (por ahora, orden natural)
            // Cuando tengamos createdAt en el preview, ordenar por eso
        }

        log.debug("Listando {} tiendas públicas activas", previews.size());
        return previews;
    }

    /**
     * Infiere la categoría del negocio a partir de la descripción o el nombre.
     * Heurística simple para los tenants iniciales.
     */
    private String inferCategory(Tenant t) {
        String desc = (t.description() != null ? t.description() : "").toLowerCase();
        String name = t.name().toLowerCase();

        if (desc.contains("restaurante") || desc.contains("comida") || desc.contains("saludable") || name.contains("idoneo")) {
            return "Restaurante Saludable";
        }
        if (desc.contains("chicha") || desc.contains("venezolana") || desc.contains("costeña") || name.contains("chicha")) {
            return "Comida Típica";
        }
        if (desc.contains("tecnología") || desc.contains("celular") || desc.contains("tech") || name.contains("tech")) {
            return "Tecnología";
        }
        return "Tienda Online";
    }
}
