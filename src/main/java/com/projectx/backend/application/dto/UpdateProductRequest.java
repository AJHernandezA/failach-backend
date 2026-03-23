package com.projectx.backend.application.dto;

import com.projectx.backend.domain.models.ProductVariant;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO para la actualización de un producto.
 */
public record UpdateProductRequest(
        String name,
        String description,
        BigDecimal price,
        BigDecimal compareAtPrice,
        List<String> images,
        String categoryId,
        int stock,
        List<ProductVariant> variants,
        int sortOrder
) {}
