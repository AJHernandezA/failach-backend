package com.projectx.backend.application.usecases;

import com.google.inject.Inject;
import com.projectx.backend.domain.exceptions.NotFoundException;
import com.projectx.backend.domain.models.Product;
import com.projectx.backend.domain.ports.in.GetProductUseCase;
import com.projectx.backend.domain.ports.out.ProductRepository;

/**
 * Implementación del caso de uso para obtener el detalle de un producto.
 */
public class GetProductUseCaseImpl implements GetProductUseCase {

    private final ProductRepository productRepository;

    @Inject
    public GetProductUseCaseImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public Product execute(String tenantId, String productId) {
        return productRepository.findById(tenantId, productId)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado: " + productId));
    }
}
