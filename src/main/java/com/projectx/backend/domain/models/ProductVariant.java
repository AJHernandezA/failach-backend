package com.projectx.backend.domain.models;

import java.math.BigDecimal;

/**
 * Variante de un producto (talla, color, etc.).
 *
 * @param variantId identificador único de la variante
 * @param name      nombre de la variante (ej: "Talla M", "Color Rojo")
 * @param price     precio de la variante (si difiere del producto base)
 * @param stock     stock específico de esta variante
 */
public record ProductVariant(
        String variantId,
        String name,
        BigDecimal price,
        int stock
) {}
