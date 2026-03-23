package com.projectx.backend.domain.models;

import java.math.BigDecimal;

/**
 * Ítem dentro de una orden (snapshot del producto al momento de la compra).
 *
 * @param productId   ID del producto
 * @param productName nombre del producto
 * @param price       precio unitario al momento de la compra
 * @param quantity    cantidad
 * @param imageUrl    imagen thumbnail
 * @param variantId   ID de variante (nullable)
 * @param variantName nombre de variante (nullable)
 */
public record OrderItem(
        String productId,
        String productName,
        BigDecimal price,
        int quantity,
        String imageUrl,
        String variantId,
        String variantName
) {

    /**
     * Retorna el subtotal de este ítem (precio × cantidad).
     */
    public BigDecimal subtotal() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }
}
