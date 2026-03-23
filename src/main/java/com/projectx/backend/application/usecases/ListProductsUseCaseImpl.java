package com.projectx.backend.application.usecases;

import com.google.inject.Inject;
import com.projectx.backend.domain.models.Page;
import com.projectx.backend.domain.models.Product;
import com.projectx.backend.domain.models.ProductFilter;
import com.projectx.backend.domain.ports.in.ListProductsUseCase;
import com.projectx.backend.domain.ports.out.ProductRepository;

/**
 * Implementación del caso de uso para listar productos con paginación y filtros.
 */
public class ListProductsUseCaseImpl implements ListProductsUseCase {

    private final ProductRepository productRepository;

    @Inject
    public ListProductsUseCaseImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public Page<Product> execute(String tenantId, ProductFilter filter) {
        return productRepository.findByTenant(tenantId, filter);
    }
}
