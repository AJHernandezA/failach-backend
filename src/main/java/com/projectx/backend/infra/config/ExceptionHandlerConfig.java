package com.projectx.backend.infra.config;

import com.projectx.backend.domain.exceptions.*;
import io.javalin.Javalin;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.UnauthorizedResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Configuración global del manejo de excepciones para Javalin.
 * Mapea cada tipo de DomainException a su código HTTP correspondiente.
 * Las excepciones inesperadas se loguean pero NO se expone el stack trace al cliente.
 */
public class ExceptionHandlerConfig {

    private static final Logger log = LoggerFactory.getLogger(ExceptionHandlerConfig.class);

    private ExceptionHandlerConfig() {
        // No instanciable
    }

    /**
     * Registra todos los handlers de excepciones en la instancia de Javalin.
     */
    public static void configure(Javalin app) {
        // Excepciones de dominio con código HTTP específico
        app.exception(BadRequestException.class, (e, ctx) -> {
            ctx.status(400).json(errorBody(e.getCode(), e.getMessage()));
        });

        app.exception(UnauthorizedException.class, (e, ctx) -> {
            ctx.status(401).json(errorBody(e.getCode(), e.getMessage()));
        });

        app.exception(ForbiddenException.class, (e, ctx) -> {
            ctx.status(403).json(errorBody(e.getCode(), e.getMessage()));
        });

        app.exception(NotFoundException.class, (e, ctx) -> {
            ctx.status(404).json(errorBody(e.getCode(), e.getMessage()));
        });

        app.exception(ConflictException.class, (e, ctx) -> {
            ctx.status(409).json(errorBody(e.getCode(), e.getMessage()));
        });

        app.exception(BusinessRuleException.class, (e, ctx) -> {
            ctx.status(422).json(errorBody(e.getCode(), e.getMessage()));
        });

        // Excepciones de Javalin (lanzadas por AuthFilter / MockAuthFilter)
        app.exception(UnauthorizedResponse.class, (e, ctx) -> {
            ctx.status(401).json(errorBody("UNAUTHORIZED", e.getMessage()));
        });

        app.exception(ForbiddenResponse.class, (e, ctx) -> {
            ctx.status(403).json(errorBody("FORBIDDEN", e.getMessage()));
        });

        // Excepción genérica: loguear y no exponer detalles al cliente
        app.exception(Exception.class, (e, ctx) -> {
            log.error("Error inesperado en {} {}: {}", ctx.method(), ctx.path(), e.getMessage(), e);
            ctx.status(500).json(errorBody("INTERNAL_ERROR", "Ha ocurrido un error interno"));
        });

        log.info("Exception handlers configurados");
    }

    /**
     * Construye el body de error estándar de la API.
     */
    private static Map<String, Object> errorBody(String code, String message) {
        return Map.of("error", Map.of("code", code, "message", message));
    }
}
