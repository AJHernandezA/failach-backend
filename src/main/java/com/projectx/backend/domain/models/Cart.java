package com.projectx.backend.domain.models;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Carrito de compras identificado por sessionId + tenantId.
 * TTL para expiración automática en DynamoDB (24h).
 *
 * @param cartId    ID del carrito (= sessionId)
 * @param tenantId  tenant al que pertenece
 * @param items     productos en el carrito
 * @param createdAt fecha de creación
 * @param updatedAt última actualización
 * @param ttl       timestamp de expiración (epoch seconds)
 */
public record Cart(
        String cartId,
        String tenantId,
        List<CartItem> items,
        Instant createdAt,
        Instant updatedAt,
        long ttl
) {

    /**
     * Retorna la cantidad total de ítems en el carrito.
     */
    public int itemCount() {
        return items.stream().mapToInt(CartItem::quantity).sum();
    }

    /**
     * Retorna el subtotal del carrito (suma de subtotales de cada ítem).
     */
    public BigDecimal subtotal() {
        return items.stream()
                .map(CartItem::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Busca un ítem por productId.
     */
    public Optional<CartItem> findItem(String productId) {
        return items.stream().filter(i -> i.productId().equals(productId)).findFirst();
    }

    /**
     * Crea un carrito vacío para una nueva sesión.
     */
    public static Cart empty(String cartId, String tenantId) {
        Instant now = Instant.now();
        long ttl = now.plusSeconds(86400).getEpochSecond();
        return new Cart(cartId, tenantId, new ArrayList<>(), now, now, ttl);
    }

    /**
     * Retorna una copia del carrito con la lista de ítems reemplazada y timestamps actualizados.
     */
    public Cart withItems(List<CartItem> newItems) {
        long newTtl = Instant.now().plusSeconds(86400).getEpochSecond();
        return new Cart(cartId, tenantId, newItems, createdAt, Instant.now(), newTtl);
    }
}
