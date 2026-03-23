package com.projectx.backend.domain.models;

/**
 * Request para búsqueda de productos con filtros.
 */
public record SearchProductsRequest(
        String tenantId,
        String query,
        String categoryId,
        Integer minPrice,
        Integer maxPrice,
        SortOption sort,
        int page,
        int size
) {
    /** Constructor con valores por defecto */
    public SearchProductsRequest(String tenantId, String query) {
        this(tenantId, query, null, null, null, SortOption.NAME, 1, 20);
    }
}
