package com.projectx.backend.domain.models;

/**
 * Entrada individual del sitemap con URL, fecha de modificación, frecuencia de cambio y prioridad.
 */
public record SitemapEntry(
    String url,
    String lastModified,
    String changeFrequency,
    double priority
) {}
