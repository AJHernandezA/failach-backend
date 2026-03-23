package com.projectx.backend.domain.models;

/**
 * Colores del tema visual de un tenant.
 * Todos los valores son strings en formato hex (#XXXXXX).
 */
public record TenantColors(
        String primary,
        String secondary,
        String accent,
        String background,
        String text
) {}
