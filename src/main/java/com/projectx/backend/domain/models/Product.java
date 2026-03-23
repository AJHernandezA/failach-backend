package com.projectx.backend.domain.models;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Entidad de dominio que representa un producto del catálogo.
 */
public record Product(
        String productId,
        String tenantId,
        String name,
        String description,
        BigDecimal price,
        BigDecimal compareAtPrice,
        List<String> images,
        String categoryId,
        String categoryName,
        int stock,
        List<ProductVariant> variants,
        ProductStatus status,
        int sortOrder,
        Instant createdAt,
        Instant updatedAt
) {}
