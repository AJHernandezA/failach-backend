package com.projectx.backend.domain.models;

/**
 * Opciones de ordenamiento para búsqueda de productos.
 */
public enum SortOption {
    PRICE_ASC,
    PRICE_DESC,
    NAME,
    NEWEST;

    /**
     * Convierte un string a SortOption. Default: NAME.
     */
    public static SortOption fromString(String value) {
        if (value == null || value.isBlank()) return NAME;
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return NAME;
        }
    }
}
