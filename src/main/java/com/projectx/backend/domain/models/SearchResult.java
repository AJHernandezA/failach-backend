package com.projectx.backend.domain.models;

import java.util.List;

/**
 * Resultado paginado de búsqueda de productos.
 */
public record SearchResult<T>(
        List<T> items,
        long total,
        int page,
        int totalPages,
        int size
) {}
