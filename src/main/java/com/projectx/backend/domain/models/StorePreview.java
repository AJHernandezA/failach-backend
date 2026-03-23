package com.projectx.backend.domain.models;

/**
 * Vista resumida de una tienda para el directorio público.
 * Solo incluye datos no sensibles.
 */
public record StorePreview(
        String tenantId,
        String businessName,
        String category,
        String description,
        String logoUrl,
        String storeUrl,
        int totalProducts
) {}
