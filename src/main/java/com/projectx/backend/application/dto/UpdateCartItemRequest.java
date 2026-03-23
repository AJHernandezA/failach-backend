package com.projectx.backend.application.dto;

/**
 * DTO para actualizar la cantidad de un ítem en el carrito.
 */
public record UpdateCartItemRequest(
        int quantity
) {}
