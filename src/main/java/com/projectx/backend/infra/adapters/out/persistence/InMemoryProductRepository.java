package com.projectx.backend.infra.adapters.out.persistence;

import com.projectx.backend.domain.models.Page;
import com.projectx.backend.domain.models.Product;
import com.projectx.backend.domain.models.ProductFilter;
import com.projectx.backend.domain.models.ProductStatus;
import com.projectx.backend.domain.ports.out.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementación en memoria del repositorio de productos.
 * Para desarrollo sin DynamoDB Local.
 */
public class InMemoryProductRepository implements ProductRepository {

    private static final Logger log = LoggerFactory.getLogger(InMemoryProductRepository.class);

    /** Key: tenantId::productId */
    private final Map<String, Product> store = new ConcurrentHashMap<>();

    private String key(String tenantId, String productId) {
        return tenantId + "::" + productId;
    }

    @Override
    public Optional<Product> findById(String tenantId, String productId) {
        log.debug("[InMemory] Buscando producto: {}:{}", tenantId, productId);
        return Optional.ofNullable(store.get(key(tenantId, productId)));
    }

    @Override
    public Page<Product> findByTenant(String tenantId, ProductFilter filter) {
        log.debug("[InMemory] Listando productos de tenant: {} con filtro: {}", tenantId, filter);

        Stream<Product> stream = store.values().stream()
                .filter(p -> p.tenantId().equals(tenantId));

        // Filtrar por status
        if (filter.status() != null) {
            stream = stream.filter(p -> p.status() == filter.status());
        }

        // Filtrar por categoría
        if (filter.categoryId() != null && !filter.categoryId().isBlank()) {
            stream = stream.filter(p -> p.categoryId().equals(filter.categoryId()));
        }

        // Búsqueda por nombre
        if (filter.search() != null && !filter.search().isBlank()) {
            String searchLower = filter.search().toLowerCase();
            stream = stream.filter(p -> p.name().toLowerCase().contains(searchLower));
        }

        // Ordenar por sortOrder y luego por nombre
        List<Product> all = stream
                .sorted(Comparator.comparingInt(Product::sortOrder).thenComparing(Product::name))
                .collect(Collectors.toList());

        long total = all.size();
        int from = filter.page() * filter.size();
        int to = Math.min(from + filter.size(), all.size());

        List<Product> items = from < all.size() ? all.subList(from, to) : List.of();

        return new Page<>(items, filter.page(), filter.size(), total);
    }

    @Override
    public List<Product> findByCategory(String tenantId, String categoryId) {
        return store.values().stream()
                .filter(p -> p.tenantId().equals(tenantId) && p.categoryId().equals(categoryId))
                .filter(p -> p.status() == ProductStatus.ACTIVE)
                .sorted(Comparator.comparingInt(Product::sortOrder))
                .collect(Collectors.toList());
    }

    @Override
    public void save(Product product) {
        log.debug("[InMemory] Guardando producto: {}:{}", product.tenantId(), product.productId());
        store.put(key(product.tenantId(), product.productId()), product);
    }

    @Override
    public void delete(String tenantId, String productId) {
        log.debug("[InMemory] Eliminando producto: {}:{}", tenantId, productId);
        store.remove(key(tenantId, productId));
    }

    @Override
    public int countByTenantId(String tenantId) {
        return (int) store.values().stream()
                .filter(p -> p.tenantId().equals(tenantId))
                .filter(p -> p.status() == ProductStatus.ACTIVE)
                .count();
    }
}
