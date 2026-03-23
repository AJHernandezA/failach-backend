package com.projectx.backend.domain.models;

import java.util.List;

/**
 * Datos agregados para renderizar la homepage de una tienda.
 * Combina config del tenant, categorías y productos destacados en un solo objeto.
 */
public record StorefrontData(
        Tenant tenant,
        List<Category> categories,
        List<Product> featuredProducts
) {}
