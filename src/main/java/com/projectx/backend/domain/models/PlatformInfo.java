package com.projectx.backend.domain.models;

/**
 * Estadísticas públicas de la plataforma.
 * Se muestra en la landing page para generar confianza.
 */
public record PlatformInfo(
        int totalStores,
        int totalProducts,
        int totalOrders
) {}
