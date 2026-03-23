package com.projectx.backend.application.usecases;

import com.google.inject.Inject;
import com.projectx.backend.application.dto.UpdateProductRequest;
import com.projectx.backend.domain.exceptions.BadRequestException;
import com.projectx.backend.domain.exceptions.NotFoundException;
import com.projectx.backend.domain.models.Product;
import com.projectx.backend.domain.models.ProductStatus;
import com.projectx.backend.domain.ports.in.UpdateProductUseCase;
import com.projectx.backend.domain.ports.out.CategoryRepository;
import com.projectx.backend.domain.ports.out.ProductRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;

/**
 * Implementación del caso de uso para actualizar un producto existente.
 */
public class UpdateProductUseCaseImpl implements UpdateProductUseCase {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Inject
    public UpdateProductUseCaseImpl(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public Product execute(String tenantId, String productId, UpdateProductRequest request) {
        // Verificar que el producto existe
        Product existing = productRepository.findById(tenantId, productId)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado: " + productId));

        validate(request);

        // Resolver nombre de categoría
        String categoryName = existing.categoryName();
        String categoryId = request.categoryId() != null ? request.categoryId() : existing.categoryId();
        if (request.categoryId() != null && !request.categoryId().equals(existing.categoryId())) {
            var category = categoryRepository.findById(tenantId, request.categoryId())
                    .orElseThrow(() -> new NotFoundException("La categoría no existe: " + request.categoryId()));
            categoryName = category.name();
        }

        // Determinar estado basado en stock
        ProductStatus status = request.stock() > 0 ? ProductStatus.ACTIVE : ProductStatus.OUT_OF_STOCK;
        // Si el producto estaba INACTIVE (soft deleted), mantener INACTIVE
        if (existing.status() == ProductStatus.INACTIVE) {
            status = ProductStatus.INACTIVE;
        }

        Product updated = new Product(
                productId,
                tenantId,
                request.name() != null ? request.name() : existing.name(),
                request.description() != null ? request.description() : existing.description(),
                request.price() != null ? request.price() : existing.price(),
                request.compareAtPrice(),
                request.images() != null ? request.images() : existing.images(),
                categoryId,
                categoryName,
                request.stock(),
                request.variants() != null ? request.variants() : existing.variants() != null ? existing.variants() : new ArrayList<>(),
                status,
                request.sortOrder(),
                existing.createdAt(),
                Instant.now()
        );

        productRepository.save(updated);
        return updated;
    }

    private void validate(UpdateProductRequest request) {
        if (request.name() != null && request.name().isBlank()) {
            throw new BadRequestException("Nombre del producto no puede estar vacío");
        }
        if (request.name() != null && request.name().length() > 200) {
            throw new BadRequestException("Nombre del producto no puede tener más de 200 caracteres");
        }
        if (request.price() != null && request.price().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("El precio debe ser mayor a 0");
        }
        if (request.images() != null && request.images().isEmpty()) {
            throw new BadRequestException("Se requiere al menos una imagen");
        }
        if (request.stock() < 0) {
            throw new BadRequestException("El stock no puede ser negativo");
        }
    }
}
