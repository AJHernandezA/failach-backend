package com.projectx.backend.domain.ports.in;

import com.projectx.backend.domain.models.SitemapData;

/**
 * Caso de uso para obtener los datos del sitemap de un tenant.
 */
public interface GetSitemapDataUseCase {
    SitemapData execute(String tenantId);
}
