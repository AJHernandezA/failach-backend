package com.projectx.backend.infra.adapters.in.controller;

import com.google.inject.Inject;
import com.projectx.backend.domain.constants.ApiConstants;
import com.projectx.backend.domain.models.LegalContent;
import com.projectx.backend.domain.ports.in.GetLegalContentUseCase;
import com.projectx.backend.domain.ports.in.UpdateLegalContentUseCase;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Controller para los endpoints de contenido legal.
 */
public class LegalController {

    private static final Logger log = LoggerFactory.getLogger(LegalController.class);

    private final GetLegalContentUseCase getLegalContent;
    private final UpdateLegalContentUseCase updateLegalContent;

    @Inject
    public LegalController(GetLegalContentUseCase getLegalContent,
                           UpdateLegalContentUseCase updateLegalContent) {
        this.getLegalContent = getLegalContent;
        this.updateLegalContent = updateLegalContent;
    }

    /**
     * Registra las rutas de contenido legal en la instancia de Javalin.
     */
    public void register(Javalin app) {
        String base = ApiConstants.API_PREFIX + "/tenants/{tenantId}/legal/{type}";

        // GET /api/v1/tenants/:tenantId/legal/:type — Obtener contenido legal
        app.get(base, ctx -> {
            String tenantId = ctx.pathParam("tenantId");
            String type = ctx.pathParam("type");
            LegalContent content = getLegalContent.execute(tenantId, type);
            ctx.json(Map.of("data", content));
        });

        // PUT /api/v1/tenants/:tenantId/legal/:type — Actualizar contenido legal (auth requerida)
        app.put(base, ctx -> {
            String tenantId = ctx.pathParam("tenantId");
            String type = ctx.pathParam("type");

            @SuppressWarnings("unchecked")
            Map<String, String> body = ctx.bodyAsClass(Map.class);
            String title = body.get("title");
            String contentText = body.get("content");

            LegalContent content = updateLegalContent.execute(tenantId, type, title, contentText);
            ctx.json(Map.of("data", content));
        });

        log.info("Legal endpoints registrados en {}/tenants/:tenantId/legal/:type", ApiConstants.API_PREFIX);
    }
}
