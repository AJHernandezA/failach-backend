package com.projectx.backend.infra.adapters.out.persistence;

import com.projectx.backend.domain.models.Category;
import com.projectx.backend.domain.ports.out.CategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Implementación en memoria del repositorio de categorías.
 * Para desarrollo sin DynamoDB Local.
 */
public class InMemoryCategoryRepository implements CategoryRepository {

    private static final Logger log = LoggerFactory.getLogger(InMemoryCategoryRepository.class);

    /** Key: tenantId::categoryId */
    private final Map<String, Category> store = new ConcurrentHashMap<>();

    private String key(String tenantId, String categoryId) {
        return tenantId + "::" + categoryId;
    }

    @Override
    public List<Category> findByTenant(String tenantId) {
        log.debug("[InMemory] Listando categorías de tenant: {}", tenantId);
        return store.values().stream()
                .filter(c -> c.tenantId().equals(tenantId))
                .sorted(Comparator.comparingInt(Category::sortOrder).thenComparing(Category::name))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Category> findById(String tenantId, String categoryId) {
        log.debug("[InMemory] Buscando categoría: {}:{}", tenantId, categoryId);
        return Optional.ofNullable(store.get(key(tenantId, categoryId)));
    }

    @Override
    public void save(Category category) {
        log.debug("[InMemory] Guardando categoría: {}:{}", category.tenantId(), category.categoryId());
        store.put(key(category.tenantId(), category.categoryId()), category);
    }
}
