package com.projectx.backend.domain.models;

import java.util.List;

/**
 * Datos del sitemap para un tenant, contiene las entradas indexables.
 */
public record SitemapData(
    List<SitemapEntry> entries
) {}
