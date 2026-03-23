package com.projectx.backend.infra.middleware;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filtro que inyecta headers de seguridad en cada response HTTP.
 * Se registra como app.after(...) para que se ejecute después de los controllers.
 * Headers basados en OWASP Secure Headers Project.
 */
public class SecurityHeadersFilter implements Handler {

    private static final Logger log = LoggerFactory.getLogger(SecurityHeadersFilter.class);

    public SecurityHeadersFilter() {
        log.info("SecurityHeadersFilter iniciado");
    }

    @Override
    public void handle(@NotNull Context ctx) throws Exception {
        // Prevenir que el contenido sea interpretado como un MIME type diferente
        ctx.header("X-Content-Type-Options", "nosniff");

        // Prevenir clickjacking
        ctx.header("X-Frame-Options", "DENY");

        // Desactivar X-XSS-Protection legacy (puede causar vulnerabilidades en browsers modernos)
        ctx.header("X-XSS-Protection", "0");

        // Controlar qué información de referencia se envía
        ctx.header("Referrer-Policy", "strict-origin-when-cross-origin");

        // Restringir APIs del browser que no usamos
        ctx.header("Permissions-Policy", "camera=(), microphone=(), geolocation=()");

        // Prevenir cache de responses con datos sensibles
        String path = ctx.path();
        if (isSensitivePath(path)) {
            ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
            ctx.header("Pragma", "no-cache");
        }
    }

    /**
     * Determina si la ruta contiene datos sensibles que no deben ser cacheados.
     * Rutas de auth, órdenes y carrito se consideran sensibles.
     */
    private boolean isSensitivePath(String path) {
        return path.contains("/auth/")
                || path.contains("/orders")
                || path.contains("/cart")
                || path.contains("/payments");
    }
}
