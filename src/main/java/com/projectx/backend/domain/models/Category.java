package com.projectx.backend.domain.models;

/**
 * Categoría de productos de un tenant.
 *
 * @param categoryId  identificador único
 * @param tenantId    tenant dueño
 * @param name        nombre de la categoría
 * @param description descripción
 * @param imageUrl    URL de la imagen representativa
 * @param sortOrder   orden de aparición en la navegación
 */
public record Category(
        String categoryId,
        String tenantId,
        String name,
        String description,
        String imageUrl,
        int sortOrder
) {}
