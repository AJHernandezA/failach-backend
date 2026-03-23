package com.projectx.backend.infra.adapters.in.controller;

import com.google.inject.Inject;
import com.projectx.backend.domain.constants.ApiConstants;
import com.projectx.backend.domain.models.SitemapData;
import com.projectx.backend.domain.ports.in.GetSitemapDataUseCase;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Controller para el endpoint de datos del sitemap.
 */
public class SitemapController {

    private static final Logger log = LoggerFactory.getLogger(SitemapController.class);

    private final GetSitemapDataUseCase getSitemapData;

    @Inject
    public SitemapController(GetSitemapDataUseCase getSitemapData) {
        this.getSitemapData = getSitemapData;
    }

    /**
     * Registra las rutas de sitemap en la instancia de Javalin.
     */
    public void register(Javalin app) {
        String base = ApiConstants.API_PREFIX + "/tenants/{tenantId}/sitemap-data";

        // GET /api/v1/tenants/:tenantId/sitemap-data — Datos para generar sitemap
        app.get(base, ctx -> {
            String tenantId = ctx.pathParam("tenantId");
            SitemapData data = getSitemapData.execute(tenantId);
            ctx.json(Map.of("data", data));
        });

        log.info("Sitemap endpoints registrados en {}/tenants/:tenantId/sitemap-data", ApiConstants.API_PREFIX);
    }
}
