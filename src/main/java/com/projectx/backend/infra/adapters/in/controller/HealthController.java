package com.projectx.backend.infra.adapters.in.controller;

import com.projectx.backend.domain.constants.ApiConstants;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;

/**
 * Controller para el endpoint de health check.
 * No requiere autenticación ni header X-Tenant-Id.
 * Usado para verificar que el servicio está activo.
 */
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    /**
     * Registra las rutas de health check en la instancia de Javalin.
     */
    public static void register(Javalin app) {
        app.get(ApiConstants.API_PREFIX + "/health", ctx -> {
            ctx.json(Map.of(
                    "status", "UP",
                    "timestamp", Instant.now().toString()
            ));
        });

        log.info("Health check endpoint registrado: GET {}/health", ApiConstants.API_PREFIX);
    }
}
