package com.projectx.backend.application.usecases;

import com.google.inject.Inject;
import com.projectx.backend.domain.exceptions.NotFoundException;
import com.projectx.backend.domain.models.Product;
import com.projectx.backend.domain.models.ProductStatus;
import com.projectx.backend.domain.ports.in.DeleteProductUseCase;
import com.projectx.backend.domain.ports.out.ProductRepository;

import java.time.Instant;

/**
 * Implementación del caso de uso para eliminar un producto (soft delete).
 * Cambia el status a INACTIVE en lugar de borrar el registro.
 */
public class DeleteProductUseCaseImpl implements DeleteProductUseCase {

    private final ProductRepository productRepository;

    @Inject
    public DeleteProductUseCaseImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public void execute(String tenantId, String productId) {
        Product existing = productRepository.findById(tenantId, productId)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado: " + productId));

        // Soft delete: cambiar status a INACTIVE
        Product deleted = new Product(
                existing.productId(),
                existing.tenantId(),
                existing.name(),
                existing.description(),
                existing.price(),
                existing.compareAtPrice(),
                existing.images(),
                existing.categoryId(),
                existing.categoryName(),
                existing.stock(),
                existing.variants(),
                ProductStatus.INACTIVE,
                existing.sortOrder(),
                existing.createdAt(),
                Instant.now()
        );

        productRepository.save(deleted);
    }
}
