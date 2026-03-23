package com.projectx.backend.application.usecases;

import com.google.inject.Inject;
import com.projectx.backend.application.dto.CreateProductRequest;
import com.projectx.backend.domain.exceptions.BadRequestException;
import com.projectx.backend.domain.exceptions.NotFoundException;
import com.projectx.backend.domain.models.Product;
import com.projectx.backend.domain.models.ProductStatus;
import com.projectx.backend.domain.ports.in.CreateProductUseCase;
import com.projectx.backend.domain.ports.out.CategoryRepository;
import com.projectx.backend.domain.ports.out.ProductRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Implementación del caso de uso para crear un producto.
 * Valida campos requeridos y verifica que la categoría exista.
 */
public class CreateProductUseCaseImpl implements CreateProductUseCase {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Inject
    public CreateProductUseCaseImpl(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public Product execute(String tenantId, CreateProductRequest request) {
        validate(request);

        // Verificar que la categoría existe
        var category = categoryRepository.findById(tenantId, request.categoryId())
                .orElseThrow(() -> new NotFoundException("La categoría no existe: " + request.categoryId()));

        // Determinar estado basado en stock
        ProductStatus status = request.stock() > 0 ? ProductStatus.ACTIVE : ProductStatus.OUT_OF_STOCK;

        Instant now = Instant.now();
        Product product = new Product(
                UUID.randomUUID().toString(),
                tenantId,
                request.name(),
                request.description() != null ? request.description() : "",
                request.price(),
                request.compareAtPrice(),
                request.images(),
                request.categoryId(),
                category.name(),
                request.stock(),
                request.variants() != null ? request.variants() : new ArrayList<>(),
                status,
                request.sortOrder(),
                now,
                now
        );

        productRepository.save(product);
        return product;
    }

    private void validate(CreateProductRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new BadRequestException("Nombre del producto es requerido");
        }
        if (request.name().length() > 200) {
            throw new BadRequestException("Nombre del producto no puede tener más de 200 caracteres");
        }
        if (request.price() == null || request.price().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("El precio debe ser mayor a 0");
        }
        if (request.images() == null || request.images().isEmpty()) {
            throw new BadRequestException("Se requiere al menos una imagen");
        }
        if (request.categoryId() == null || request.categoryId().isBlank()) {
            throw new BadRequestException("La categoría es requerida");
        }
        if (request.stock() < 0) {
            throw new BadRequestException("El stock no puede ser negativo");
        }
    }
}
