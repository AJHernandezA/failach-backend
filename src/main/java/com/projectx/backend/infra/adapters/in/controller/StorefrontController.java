package com.projectx.backend.infra.adapters.in.controller;

import com.google.inject.Inject;
import com.projectx.backend.domain.constants.ApiConstants;
import com.projectx.backend.domain.models.StorefrontData;
import com.projectx.backend.domain.ports.in.GetStorefrontDataUseCase;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Controller para el endpoint de storefront (homepage).
 * Retorna toda la data necesaria para renderizar la homepage en un solo request.
 */
public class StorefrontController {

    private static final Logger log = LoggerFactory.getLogger(StorefrontController.class);

    private final GetStorefrontDataUseCase getStorefrontData;

    @Inject
    public StorefrontController(GetStorefrontDataUseCase getStorefrontData) {
        this.getStorefrontData = getStorefrontData;
    }

    /**
     * Registra las rutas de storefront en Javalin.
     */
    public void register(Javalin app) {
        // GET /api/v1/tenants/:tenantId/storefront — Data completa para homepage
        app.get(ApiConstants.API_PREFIX + "/tenants/{tenantId}/storefront", ctx -> {
            String tenantId = ctx.pathParam("tenantId");
            StorefrontData data = getStorefrontData.execute(tenantId);
            ctx.json(Map.of("data", data));
        });

        log.info("Storefront endpoint registrado: GET {}/tenants/:tenantId/storefront", ApiConstants.API_PREFIX);
    }
}
