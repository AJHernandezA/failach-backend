package com.projectx.backend.domain.models;

/**
 * Estado de un producto en el catálogo.
 * ACTIVE: visible y disponible para compra.
 * INACTIVE: oculto del catálogo público (soft delete).
 * OUT_OF_STOCK: visible pero no disponible para compra.
 */
public enum ProductStatus {
    ACTIVE,
    INACTIVE,
    OUT_OF_STOCK
}
