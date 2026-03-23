package com.projectx.backend.application.dto;

/**
 * DTO para agregar un producto al carrito.
 */
public record AddToCartRequest(
        String productId,
        int quantity,
        String variantId
) {}
