package com.projectx.backend.domain.models;

import java.math.BigDecimal;

/**
 * Ítem dentro del carrito de compras.
 * Precio e imagen se desnormalizan al momento de agregar (snapshot).
 *
 * @param productId   ID del producto
 * @param productName nombre del producto (desnormalizado)
 * @param price       precio unitario al momento de agregar
 * @param quantity    cantidad (mínimo 1)
 * @param imageUrl    imagen thumbnail (desnormalizada)
 * @param variantId   ID de variante (nullable)
 * @param variantName nombre de variante (nullable)
 */
public record CartItem(
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

    /**
     * Retorna una copia de este ítem con la cantidad actualizada.
     */
    public CartItem withQuantity(int newQuantity) {
        return new CartItem(productId, productName, price, newQuantity, imageUrl, variantId, variantName);
    }
}
