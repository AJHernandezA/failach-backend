package com.projectx.backend.domain.models;

/**
 * Filtro para buscar productos. Se usa en ListProductsUseCase.
 *
 * @param categoryId filtrar por categoría (opcional)
 * @param search     búsqueda por nombre (opcional)
 * @param status     filtrar por estado (default ACTIVE para público)
 * @param page       número de página (0-based)
 * @param size       tamaño de página (default 20)
 */
public record ProductFilter(
        String categoryId,
        String search,
        ProductStatus status,
        int page,
        int size
) {
    /** Constructor con valores por defecto para consulta pública */
    public ProductFilter() {
        this(null, null, ProductStatus.ACTIVE, 0, 20);
    }
}
