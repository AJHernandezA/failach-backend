package com.projectx.backend.domain.ports.in;

import com.projectx.backend.domain.models.Product;
import com.projectx.backend.domain.models.SearchProductsRequest;
import com.projectx.backend.domain.models.SearchResult;

/**
 * Puerto de entrada para búsqueda de productos con filtros.
 */
public interface SearchProductsUseCase {
    SearchResult<Product> execute(SearchProductsRequest request);
}
